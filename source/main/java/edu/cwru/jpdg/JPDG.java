package edu.cwru.jpdg;

import java.util.*;
import soot.options.Options;

import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;

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
        // O.set_app(true);


        S.loadNecessaryClasses();
        soot.PackManager.v().runPacks();




        soot.util.Chain<soot.SootClass> classes = S.getApplicationClasses();

        System.out.println();
        for (soot.SootClass c : classes) {
            System.out.println(c);
            for (soot.SootMethod m : c.getMethods()) {
                System.out.print("  ");
                System.out.println(m);
                System.out.print("    ");
                System.out.println(m.retrieveActiveBody().getClass());
            }
            System.out.println();
        }

        System.out.println();
        for (soot.SootMethod m : S.getEntryPoints()) {
            System.out.println(m);
        }

        System.out.println();
        System.out.println(S.getCallGraph());
    }
}
