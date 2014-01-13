package edu.cwru.jpdg;

import java.util.*;
import soot.options.Options;

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

        soot.Scene S = soot.Scene.v();
        Options O = Options.v();
        System.out.println(S);
        System.out.println(O);
        O.set_soot_classpath(cp);
        O.set_process_dir(dirs);
        S.loadNecessaryClasses();
    }
}
