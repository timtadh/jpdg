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

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

import soot.toolkits.graph.pdg.EnhancedBlockGraph;
import soot.toolkits.graph.ExpandedBlockGraph;

import edu.cwru.jpdg.pDG_Builder;
import edu.cwru.jpdg.graph.Graph;
import edu.cwru.jpdg.label.*;

public class api_examples_test {

    soot.SootClass cfg_klass = Javac.classes("test.pDG.Buffer").get("test.pDG.Buffer");

    public pDG_Builder get_builder(String method) throws pDG_Builder.Error {
        pDG_Builder pDG = pDG_Builder.test_instance();
        pDG.g = new Graph();
        pDG.lm = new OpLabels();
        pDG.klass = cfg_klass;
        pDG.method = cfg_klass.getMethodByName(method);
        pDG.body = pDG.method.retrieveActiveBody();
        assert pDG.body != null;
        pDG.cfg = new ExpandedBlockGraph(pDG.body);
        pDG.init();
        return pDG;
    }

    @Test
    public void dotty_main_CFG() throws pDG_Builder.Error {
        pDG_Builder pDG = get_builder("main");
        pDG.build_cfg();
        Dotty.graphviz("test.pDG.Buffer.main.cfg", Dotty.dotty(pDG.g.Serialize()));
    }

    @Test
    public void dotty_read_CFG() throws pDG_Builder.Error {
        pDG_Builder pDG = get_builder("read");
        pDG.build_cfg();
        Dotty.graphviz("test.pDG.Buffer.read.cfg", Dotty.dotty(pDG.g.Serialize()));
    }

    @Test
    public void dotty_put_CFG() throws pDG_Builder.Error {
        pDG_Builder pDG = get_builder("put");
        pDG.build_cfg();
        Dotty.graphviz("test.pDG.Buffer.put.cfg", Dotty.dotty(pDG.g.Serialize()));
    }

    @Test
    public void dotty_putAfter_CFG() throws pDG_Builder.Error {
        pDG_Builder pDG = get_builder("putAfter");
        pDG.build_cfg();
        Dotty.graphviz("test.pDG.Buffer.putAfter.cfg", Dotty.dotty(pDG.g.Serialize()));
    }
}

