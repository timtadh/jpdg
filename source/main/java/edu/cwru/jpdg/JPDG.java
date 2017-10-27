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


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

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
import edu.cwru.jpdg.label.LabelMaker;
import edu.cwru.jpdg.label.ExpressionTreeLabels;
import edu.cwru.jpdg.label.InstructionLabels;
import edu.cwru.jpdg.label.OpLabels;

public class JPDG {

    public static void main(String[] argv) throws pDG_Builder.Error {
        final Option helpOpt = new Option("h", "help", false, "print this message");
        final Option outputOpt = new Option("o", "output", true, "output file location");
        final Option baseOpt = new Option("d", "dirs", true, "base directories to analyze");
        final Option excludeOpt = new Option("e", "exclude", true, "exclude these directories");
        final Option classOpt = new Option("c", "classpath", true, "classpath for soot");
        final Option labelOpt = new Option("l", "label-type", true, "label type, valid choices are: expr-tree, inst");
        final org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();

        options.addOption(helpOpt);
        options.addOption(outputOpt);
        options.addOption(baseOpt);
        options.addOption(classOpt);
        options.addOption(labelOpt);
        options.addOption(excludeOpt);

        String cp = null;
        List<String> dirs = new ArrayList<String>();
        String label_type = "expr-tree";
        String output_file = null;
        List<String> excluded = new ArrayList<String>();

        try {
            GnuParser parser = new GnuParser();
            CommandLine line = parser.parse(options, argv);

            if (line.hasOption(helpOpt.getLongOpt())) {
                Usage(options);
            }

            cp = line.getOptionValue(classOpt.getLongOpt());
            String[] dir_arr = line.getOptionValues(baseOpt.getLongOpt());
            label_type = line.getOptionValue(labelOpt.getLongOpt());
            output_file = line.getOptionValue(outputOpt.getLongOpt());
            String[] ex = line.getOptionValues(excludeOpt.getLongOpt());
            if (ex != null) {
                excluded = Arrays.asList(ex);
            }
            if (dir_arr != null) {
                dirs = Arrays.asList(dir_arr);
            }
        } catch (final MissingOptionException e) {
            System.err.println(e.getMessage());
            Usage(options);
        } catch (final UnrecognizedOptionException e) {
            System.err.println(e.getMessage());
            Usage(options);
        } catch (final ParseException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        soot.Scene S = runSoot(cp, dirs, excluded);
        writeGraph(build_PDG(S, excluded, label_type), output_file);
    }

    public static void Usage(org.apache.commons.cli.Options options) {
        new HelpFormatter().printHelp("jpdg", "", options, "");
        System.exit(1);
    }

    public static soot.Scene runSoot(String cp, List<String> dirs, List<String> excluded) {
        soot.G.reset();
        addPacks();
        soot.Scene S = soot.Scene.v();
        Options O = Options.v();
        configure_and_run_soot(S, O, cp, dirs, excluded);

        // System.out.println();
        // S.getCallGraph();

        return S;
    }

    public static void configure_and_run_soot(soot.Scene S, Options O, String cp, List<String> dirs, List<String> excluded) {
        O.set_soot_classpath(cp);
        O.set_process_dir(dirs);
        // O.set_exclude(excluded);
        // O.set_no_bodies_for_excluded(true);
        if (false) {
            O.set_whole_program(true);
            // O.setPhaseOption("cg.spark", "enabled:true");
            O.setPhaseOption("cg.cha", "enabled:true");
            O.setPhaseOption("cg.spark", "enabled:false");
            O.setPhaseOption("cg.paddle", "enabled:false");
        }
        // O.setPhaseOption("wjtp", "enabled:true");
        // O.setPhaseOption("wjtp.myTrans", "enabled:true");
        // O.setPhaseOption("jop", "enabled:true");
        // O.setPhaseOption("bb", "enabled:false");
        O.set_output_format(O.output_format_jimple);
        O.set_keep_line_number(true);
        O.set_allow_phantom_refs(true);
        O.set_keep_offset(true);
        O.set_ignore_resolution_errors(true);
        O.set_verbose(false);
        if (dirs.size() == 1 && dirs.get(0).endsWith("apk")) {
            System.out.println("setting up as an android scan");
            O.set_force_android_jar("/home/hendersont/srcs/android-sdk-linux/platforms/android-21/");
            O.set_src_prec(O.src_prec_apk);
        }
        // O.set_app(true);

        S.loadNecessaryClasses();
        soot.PackManager.v().runPacks();
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

    public static Graph build_PDG(soot.Scene S, List<String> excluded, String label_type) throws pDG_Builder.Error {
        LabelMaker lm = null;
        if (label_type.equals("inst")) {
            lm = new InstructionLabels();
        } else if (label_type.equals("expr-tree")) {
            lm = new ExpressionTreeLabels();
        } else if (label_type.equals("op")) {
            lm = new OpLabels();
        } else {
            throw new RuntimeException("uknown label type: " + label_type);
        }
        System.out.println("LABEL TYPE " + label_type + " " + lm);
        soot.util.Chain<soot.SootClass> classes = S.getApplicationClasses();
        CallGraph cg = null;
        if (false) {
            cg = S.getCallGraph();
        }
        return PDG_Builder.build(cg, lm, classes, excluded);
    }

    public static void writeGraph(Graph g, String path) {
        FileOutputStream s = null;
        try {
            s = new FileOutputStream(path);
            g.Write(s);
        } catch (IOException ex) {
            System.err.println(ex);
        } finally {
           try {s.close();} catch (Exception ex) {}
        }
    }
}

