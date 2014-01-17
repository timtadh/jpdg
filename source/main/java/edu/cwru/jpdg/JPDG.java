package edu.cwru.jpdg;

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
        try {
            System.out.println(g.Serialize());
        } catch (java.io.IOException e) {
            throw new RuntimeException("biz", e);
        }
    }
}
