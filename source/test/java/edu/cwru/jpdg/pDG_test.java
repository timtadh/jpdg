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

import edu.cwru.jpdg.pDG_Builder;
import edu.cwru.jpdg.graph.Graph;
import edu.cwru.jpdg.label.*;

public class pDG_test {

    soot.SootClass cfg_klass = Javac.classes("test.pDG.CFG").get("test.pDG.CFG");

    public pDG_Builder fib_pDG_Builder() {
        pDG_Builder pDG = pDG_Builder.test_instance();
        pDG.g = new Graph();
        pDG.lm = new ExpressionTreeLabels();
        pDG.klass = cfg_klass;
        pDG.method = cfg_klass.getMethodByName("fib");
        pDG.body = pDG.method.retrieveActiveBody();
        assert pDG.body != null;
        pDG.cfg = new EnhancedBlockGraph(pDG.body);
        pDG.init();
        return pDG;
    }

    @Test
    public void test_fib_cfg() {
        pDG_Builder pDG = fib_pDG_Builder();
        pDG.build_cfg();
        Dotty.graphviz("fib_cfg", Dotty.dotty(pDG.g.Serialize()));

        assertThat(pDG.g.hasEdge(0, 1, "cfg"), is(true));
        assertThat(pDG.g.hasEdge(1, 2, "cfg"), is(true));
        assertThat(pDG.g.hasEdge(1, 3, "cfg"), is(true));
        assertThat(pDG.g.hasEdge(2, 6, "cfg"), is(true));
        assertThat(pDG.g.hasEdge(3, 4, "cfg"), is(true));
        assertThat(pDG.g.hasEdge(4, 6, "cfg"), is(true));
        assertThat(pDG.g.hasEdge(4, 5, "cfg"), is(true));
        assertThat(pDG.g.hasEdge(5, 4, "cfg"), is(true));
    }

    @Test
    public void test_fib_cdg() {
        pDG_Builder pDG = fib_pDG_Builder();
        pDG.build_cdg();
        Dotty.graphviz("fib_cdg", Dotty.dotty(pDG.g.Serialize()));

        assertThat(pDG.g.hasEdge(0, 1, "cdg"), is(true));
        assertThat(pDG.g.hasEdge(1, 2, "cdg"), is(true));
        assertThat(pDG.g.hasEdge(1, 3, "cdg"), is(true));
        assertThat(pDG.g.hasEdge(0, 6, "cdg"), is(true));
        assertThat(pDG.g.hasEdge(1, 4, "cdg"), is(true));
        assertThat(pDG.g.hasEdge(4, 5, "cdg"), is(true));
        assertThat(pDG.g.hasEdge(4, 4, "cdg"), is(true));
    }

    @Test
    public void test_fib_ddg() {
        pDG_Builder pDG = fib_pDG_Builder();
        pDG.build_ddg();
        Dotty.graphviz("fib_ddg", Dotty.dotty(pDG.g.Serialize()));

        assertThat(pDG.g.hasEdge(1, 5, "ddg"), is(true));
        assertThat(pDG.g.hasEdge(1, 4, "ddg"), is(true));
        assertThat(pDG.g.hasEdge(1, 6, "ddg"), is(true));
        assertThat(pDG.g.hasEdge(2, 6, "ddg"), is(true));
        assertThat(pDG.g.hasEdge(3, 4, "ddg"), is(true));
        assertThat(pDG.g.hasEdge(3, 5, "ddg"), is(true));
        assertThat(pDG.g.hasEdge(5, 4, "ddg"), is(true));
        assertThat(pDG.g.hasEdge(5, 6, "ddg"), is(true));
    }

    @Test
    public void write_fib_pDG() {
        pDG_Builder pDG = fib_pDG_Builder();
        pDG.build_cdg();
        pDG.build_ddg();
        Dotty.graphviz("fib_pDG", Dotty.dotty(pDG.g.Serialize()));
    }
}

