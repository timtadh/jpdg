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
import java.io.*;
import java.nio.charset.Charset;

import soot.options.Options;

import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;

import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.MHGPostDominatorsFinder;
import soot.toolkits.graph.DominatorNode;
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

public class JPDG {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Must supply classpath and base-dir");
            System.exit(1);
        }
        String cp = args[0];
        String base_dir = args[1];
        List<String> dirs = new ArrayList<String>();
        dirs.add(base_dir);

        soot.Scene S = runSoot(cp, dirs);
        writeGraph(build_PDG(S), "out.pdg");
    }

    public static soot.Scene runSoot(String cp, List<String> dirs) {
        addPacks();
        soot.Scene S = soot.Scene.v();
        Options O = Options.v();

        O.set_soot_classpath(cp);
        O.set_process_dir(dirs);
        // O.set_exclude(excluded);
        // O.set_whole_program(true);
        // O.setPhaseOption("cg.spark", "enabled:true");
        // O.setPhaseOption("wjtp", "enabled:true");
        // O.setPhaseOption("wjtp.myTrans", "enabled:true");
        // O.setPhaseOption("jop", "enabled:true");
        // O.setPhaseOption("bb", "enabled:false");
        O.set_output_format(O.output_format_jimple);
        O.set_keep_line_number(true);
        O.set_keep_offset(true);
        O.set_ignore_resolution_errors(true);
        O.set_verbose(false);
        // O.set_app(true);

        S.loadNecessaryClasses();
        soot.PackManager.v().runPacks();

        // System.out.println();
        // for (soot.SootMethod m : S.getEntryPoints()) {
            // System.out.println(m);
        // }

        // System.out.println();
        // S.getCallGraph();

        return S;
    }

    public static void addPacks() {
        soot.PackManager.v().getPack("wjtp").add(new soot.Transform("wjtp.myTrans", new soot.SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName, Map options) {
                CHATransformer.v().transform();
                CallGraph cg = soot.Scene.v().getCallGraph();
            }
        }));
    }

    public static Graph build_PDG(soot.Scene S) {
        soot.util.Chain<soot.SootClass> classes = S.getApplicationClasses();
        return PDG_Builder.build(classes);
    }

    public static void writeGraph(Graph g, String path) {
        byte[] graph = g.Serialize().getBytes(Charset.forName("UTF-8"));

        FileOutputStream s = null;
        try {
            s = new FileOutputStream(path);
            s.write(graph);
        } catch (IOException ex) {
            System.err.println(ex);
        } finally {
           try {s.close();} catch (Exception ex) {}
        }
    }
}

