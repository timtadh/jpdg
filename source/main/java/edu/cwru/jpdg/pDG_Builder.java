package edu.cwru.jpdg;

/* Tim Henderson (tadh@case.edu)
 *
 * This file is part of jpdg a library to generate Program Dependence Graphs
 * from JVM bytecode.
 *
 * Copyright (c) 2014, Tim Henderson, Case Western Reserve University
 *   Cleveland, Ohio 44106
 *   All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor,
 *   Boston, MA  02110-1301
 *   USA
 * or retrieve version 2.1 at their website:
 *   http://www.gnu.org/licenses/lgpl-2.1.html
 */ 

import java.util.*;

import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;

import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.MHGPostDominatorsFinder;
import soot.toolkits.graph.DominatorNode;
import soot.toolkits.graph.DominatorTree;
import soot.toolkits.graph.CytronDominanceFrontier;
import soot.toolkits.graph.pdg.EnhancedBlockGraph;
import soot.toolkits.graph.pdg.MHGDominatorTree;

import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

import soot.jimple.DefinitionStmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.internal.JimpleLocalBox;

import edu.cwru.jpdg.graph.Graph;
import edu.cwru.jpdg.label.LabelMaker;

public class pDG_Builder {

    public static class Error extends Exception {
        public Error(String msg) {
            super(msg);
        }
    }

    public static class ParameterNotFound extends Error {
        soot.Unit unit;
        soot.Value param;
        public ParameterNotFound(soot.Unit unit, soot.Value param) {
            super(String.format("Could not find %s[%s] in %s[%s]", param, param.getClass(), unit, unit.getClass()));
            this.unit = unit;
            this.param = param;
        }
    }

    public static class SootError extends Error {
        RuntimeException e;
        public SootError(RuntimeException e, String msg) {
            super(msg);
            this.e = e;
        }
        public static SootError create(RuntimeException e) {
            java.io.StringWriter writer = new java.io.StringWriter();
            java.io.PrintWriter printWriter = new java.io.PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();
            String stackTrace = writer.toString();
            return new SootError(e, String.format("%s\n%s", e, stackTrace));
        }
    }

    public Graph g;
    LabelMaker lm;
    soot.SootClass klass;
    soot.SootMethod method;
    soot.Body body;
    BlockGraph cfg;
    UnitGraph ucfg;

    public int entry_uid;
    HashMap<Integer,Integer> block_uids = new HashMap<Integer,Integer>();
    HashMap<soot.Unit,Block> unit_to_blk = new HashMap<soot.Unit,Block>();

    public ddg_Builder ddg_builder;


    public static void build(LabelMaker lm, Graph g, soot.SootClass c, soot.SootMethod m, soot.Body body, BlockGraph cfg) throws Error {
        pDG_Builder self = new pDG_Builder(lm, g, c, m, body, cfg);
        self.build_pDG();
    }

    static pDG_Builder test_instance() {
        return new pDG_Builder();
    }

    private pDG_Builder() {}

    private pDG_Builder(LabelMaker lm, Graph g, soot.SootClass c, soot.SootMethod m, soot.Body body, BlockGraph cfg) throws Error {
        this.lm = lm;
        this.g = g;
        this.klass = c;
        this.method = m;
        this.body = body;
        this.cfg = cfg;
        this.init();
    }

    void init() throws Error {
        this.assign_uids();
        this.map_units_to_blks();
        this.ddg_builder = new ddg_Builder();
    }

    void build_pDG() throws Error {
        // this.build_cfg();
        this.build_cdg();
        this.build_ddg();
    }

    void assign_uids() {
        this.entry_uid = g.addNode(
            "entry-"+method.getSignature(), "",
            klass.getPackageName(), klass.getName(), method.getSignature(),
            "entry",
            method.getJavaSourceStartLineNumber(),
            method.getJavaSourceStartColumnNumber(),
            method.getJavaSourceStartLineNumber(),
            method.getJavaSourceStartColumnNumber()
        );
        for (Iterator<Block> i = cfg.iterator(); i.hasNext(); ) {
            Block b = i.next();
            int uid = g.addNode(
                lm.label(this, b),
                b.toString(),
                klass.getPackageName(), klass.getName(), method.getSignature(),
                lm.nodeType(b),
                b.getHead().getJavaSourceStartLineNumber(),
                b.getHead().getJavaSourceStartColumnNumber(),
                b.getTail().getJavaSourceStartLineNumber(),
                b.getTail().getJavaSourceStartColumnNumber()
            );
            block_uids.put(b.getIndexInMethod(), uid);
            lm.postLabel(this, uid, b);
        }
    }

    void map_units_to_blks() {
        for (Iterator<Block> i = cfg.iterator(); i.hasNext(); ) {
            Block b = i.next();
            int b_uid = block_uids.get(b.getIndexInMethod());
            for (Iterator<soot.Unit> iu = b.iterator(); iu.hasNext(); ) {
                soot.Unit u = iu.next();
                unit_to_blk.put(u, b);
            }
        }
    }

    void build_cfg() {
        // add a path from the entry to each head in the graph
        for (Block head : cfg.getHeads()) {
            int head_uid = block_uids.get(head.getIndexInMethod());
            g.addEdge(entry_uid, head_uid, "cfg");
        }

        // add cfg edges
        for (Iterator<Block> i = cfg.iterator(); i.hasNext(); ) {
            Block b = i.next();
            int uid_i = block_uids.get(b.getIndexInMethod());
            for (Block s : b.getSuccs()) {
                int uid_s = block_uids.get(s.getIndexInMethod());
                g.addEdge(uid_i, uid_s, "cfg");
            }
        }
    }

    void build_cdg() throws Error {

        MHGPostDominatorsFinder pdf = new MHGPostDominatorsFinder(cfg);
        MHGDominatorTree pdom_tree = new MHGDominatorTree(pdf);
        CytronDominanceFrontier rdf = null;
        try {
            rdf = new CytronDominanceFrontier(pdom_tree);
        } catch (RuntimeException e) {
            throw SootError.create(e);
        }

        // initialize a map : uids -> bool indicating if there is a parent for
        // the block in the cdg. If there isn't it is dependent on the dummy
        // entry node.
        HashMap<Integer,Boolean> has_parent = new HashMap<Integer,Boolean>();
        for (Iterator<Block> i = cfg.iterator(); i.hasNext(); ) {
            Block y = i.next();
            int uid_y = block_uids.get(y.getIndexInMethod());
            has_parent.put(uid_y, false);
        }

        // using Cytrons algorithm for each block, y, is dependent on another
        // block, x, if x appears in y post-domanance frontier.
        for (Iterator<Block> i = cfg.iterator(); i.hasNext(); ) {
            Block y = i.next();
            int uid_y = block_uids.get(y.getIndexInMethod());
            try {
                for (Object o : rdf.getDominanceFrontierOf(pdom_tree.getDode(y))) {
                    Block x = ((Block)((DominatorNode)o).getGode());
                    int uid_x = block_uids.get(x.getIndexInMethod());
                    if (uid_x != uid_y) {
                        g.addEdge(uid_x, uid_y, "");
                        has_parent.put(uid_y, true);
                    }
                }
            } catch (java.lang.RuntimeException e) {
                System.err.println("CDG builder swallowing > " + e);
            }
        }

        // finally all of those blocks without parents need to become dependent
        // on the entry to the procedure.
        for (Iterator<Block> i = cfg.iterator(); i.hasNext(); ) {
            Block y = i.next();
            int uid_y = block_uids.get(y.getIndexInMethod());
            if (!has_parent.get(uid_y)) {
                g.addEdge(entry_uid, uid_y, "");
            }
        }
    }

    void build_ddg() throws Error {
        ddg_builder.build();
    }

    public int get_param_number(soot.Unit u, soot.Value value) throws Error {
        int i = 0;
        for (soot.ValueBox vb : u.getUseBoxes()) {
            soot.Value v = vb.getValue();
            if (value instanceof soot.jimple.DoubleConstant && v instanceof soot.jimple.DoubleConstant) {
                soot.jimple.DoubleConstant dc = (soot.jimple.DoubleConstant)v;
                soot.jimple.DoubleConstant value_dc = (soot.jimple.DoubleConstant)value;
                double d = dc.value;
                double value_d = value_dc.value;
                // NANs are a killer. We have do this because soot is broken for
                // NAN constants
                if (d == d && d == value_d) {
                    return i;
                } else if (d != d && value_d != value_d) {
                    return i;
                }
            } else if (vb.getValue().equivTo(value)) {
                return i;
            }
            i++;
        }
        throw new ParameterNotFound(u, value);
    }

    public class ddg_Builder {

        public BriefUnitGraph bug = new BriefUnitGraph(body);
        public SimpleLiveLocals sll = new SimpleLiveLocals(bug);
        public SmartLocalDefs sld = new SmartLocalDefs(bug, sll);
        public SimpleLocalUses slu = new SimpleLocalUses(bug, sld);
        public HashMap<Integer,HashMap<Integer,List<DefinitionStmt>>> defining_stmts = new HashMap<Integer,HashMap<Integer,List<DefinitionStmt>>>();

        ddg_Builder() throws Error {
            for (Iterator<Block> i = cfg.iterator(); i.hasNext(); ) {
                Block b = i.next();
                int uid_b = block_uids.get(b.getIndexInMethod());
                defining_stmts.put(uid_b, find_def_stmts(b));
            }
        }

        HashMap<Integer,List<DefinitionStmt>> find_def_stmts(Block b) {
            // For each stmt which defines a value, associate it with that value
            // in the `definit_stmts` variable.
            HashMap<Integer,List<DefinitionStmt>> def_stmts = new HashMap<Integer,List<DefinitionStmt>>();
            for (Iterator<soot.Unit> it = b.iterator(); it.hasNext(); ) {
                soot.Unit u = it.next();
                if (u instanceof DefinitionStmt) {
                    DefinitionStmt def_stmt = (DefinitionStmt)u;
                    soot.Local var = null;
                    try {
                        var  = (soot.Local)def_stmt.getLeftOp();
                    } catch (java.lang.ClassCastException e) {
                        // It will be a field reference if it is not a local.
                        // In the future we want to dataflow through field
                        // references however that will always be handled by
                        // another method as it will be inter-procedural.
                        // System.err.println(String.format("LeftOp was not a local, %s", def_stmt.getLeftOp()));
                        continue;
                    }
                    if (!def_stmts.containsKey(var.getNumber())) {
                        def_stmts.put(var.getNumber(), new ArrayList<DefinitionStmt>());
                    }
                    def_stmts.get(var.getNumber()).add(def_stmt);
                }
            }
            return def_stmts;
        }

        void build() throws Error {
            // System.err.println("building ddg for " + klass.getPackageName() + " " + klass.getName() + " " + method.getName());

            // For each block, find the blocks which are data dependent.

            for (Iterator<Block> i = cfg.iterator(); i.hasNext(); ) {
                find_data_dependencies(i.next());
            }
        }

        void find_data_dependencies(Block b) throws Error {
            int uid_b = block_uids.get(b.getIndexInMethod());
            HashMap<Integer,List<DefinitionStmt>> def_stmts = defining_stmts.get(uid_b);

            // For each live-variable at the end of the block, find its defining
            // stmts. For each defining stmt identify the "upward exposed uses"
            // and for each use attach a dependence edge between this block and
            // that one.
            List<soot.Local> values = sll.getLiveLocalsAfter(b.getTail());
            for (soot.Local value : values) {
                if (!def_stmts.containsKey(value.getNumber())) { continue; }
                for (DefinitionStmt def_stmt : def_stmts.get(value.getNumber())) {
                    List<UnitValueBoxPair> uses = slu.getUsesOf(def_stmt);
                    for (UnitValueBoxPair u : uses) {
                        Block ub = unit_to_blk.get(u.unit);
                        int uid_ub = block_uids.get(ub.getIndexInMethod());
                        int param = get_param_number(u.unit, value);
                        String label = String.format("%s:%d", value.getType(), param);
                        // In the future we may want to consider allowing
                        // this if the block is in a loop and the dependency
                        // is loop carried.
                        if (uid_b != uid_ub) {
                            g.addEdge(uid_b, uid_ub, label);
                        }
                    }
                }
            }
        }

    }
}
