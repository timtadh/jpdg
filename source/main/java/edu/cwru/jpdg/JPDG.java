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

import soot.options.Options;

import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;

import soot.toolkits.graph.Block;
import soot.toolkits.graph.ExceptionalBlockGraph;

import edu.cwru.jpdg.graph.Graph;

public class JPDG {
    public static void main(String[] args) {
        System.out.println("x");
        for (String a : args) {
            System.out.print("  "); System.out.println(a);
        }
        if (args.length != 2) {
            System.out.println("Must supply classpath and base-dir");
            System.exit(1);
        }
        String cp = args[0];
        String base_dir = args[1];
        List<String> dirs = new ArrayList<String>();
        dirs.add(base_dir);
        List<String> excluded = new ArrayList<String>();
        // excluded.add("java");
        // excluded.add("sun");
        // excluded.add("org.hamcrest");

        soot.PackManager.v().getPack("wjtp").add(new soot.Transform("wjtp.myTrans", new soot.SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName, Map options) {
                CHATransformer.v().transform();
                CallGraph cg = soot.Scene.v().getCallGraph();
            }
        }));

        soot.Scene S = soot.Scene.v();
        Options O = Options.v();

        O.set_soot_classpath(cp);
        O.set_process_dir(dirs);
        O.set_exclude(excluded);
        O.set_whole_program(true);
        O.setPhaseOption("cg.spark", "enabled:true");
        O.setPhaseOption("wjtp", "enabled:true");
        O.setPhaseOption("wjtp.myTrans", "enabled:true");
        O.setPhaseOption("jop", "enabled:true");
        O.setPhaseOption("bb", "enabled:false");
        O.set_output_format(O.output_format_shimple);
        // O.set_app(true);


        S.loadNecessaryClasses();
        soot.PackManager.v().runPacks();




        soot.util.Chain<soot.SootClass> classes = S.getApplicationClasses();

        Graph<Block> g = new Graph<Block>();

        System.out.println();
        for (soot.SootClass c : classes) {
            System.out.println(c);
            for (soot.SootMethod m : c.getMethods()) {
                System.out.print("  ");
                System.out.println(m);
                System.out.print("    ");
                soot.Body body = m.retrieveActiveBody();
                System.out.println(body.getClass());
                ExceptionalBlockGraph ebg = new ExceptionalBlockGraph(body);
                for (Iterator<Block> i = ebg.iterator(); i.hasNext(); ) {
                    Block b = i.next();
                    int uid = g.addNode(b);
                    List<Block> succ = b.getSuccs();
                    List<Block> pred = b.getPreds();
                    System.out.print("      block "); System.out.println(b.getIndexInMethod());
                    System.out.print("        succ");
                    for (Block s : succ) {
                        System.out.print(" ");
                        System.out.print(s.getIndexInMethod());
                        g.addEdge(uid, g.nodeUID(s), "cfg");
                    }
                    System.out.println();
                    System.out.print("        pred");
                    for (Block p : pred) {
                        System.out.print(" ");
                        System.out.print(p.getIndexInMethod());
                    }
                    System.out.println();
                }
            }
            System.out.println();
        }

        System.out.println();
        for (soot.SootMethod m : S.getEntryPoints()) {
            System.out.println(m);
        }

        System.out.println();
        S.getCallGraph();

        System.out.println();
        System.out.println(g.Serialize());
    }
}
