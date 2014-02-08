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

import edu.cwru.jpdg.graph.Graph;
import edu.cwru.jpdg.pDG_Builder;

public class cfg_test {

    @Test
    public void test_fib_cfg() {
        pDG_Builder pDG = pDG_Builder.test_instance();
        pDG.g = new Graph();
        soot.SootClass cfg_klass = Javac.classes("test.cfg.CFG").get("test.cfg.CFG");
        pDG.klass = cfg_klass;
        pDG.method = cfg_klass.getMethodByName("fib");
        pDG.body = pDG.method.retrieveActiveBody();
        pDG.cfg = new EnhancedBlockGraph(pDG.body);
        pDG.assign_uids();
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
}

