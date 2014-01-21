package edu.cwru.jpdg.graph;

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

import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class test_Graph {

    String GS = "{\"edges\":{\"cfg\":[[1,2],[2],[]]},\"node_labels\":[\"a\",\"b\",\"c\"]}";

    @Test
    public void build_graph() {
        Graph g = new Graph();
        int a = g.addNode(new Object(), "a");
        int b = g.addNode(new Object(), "b");
        int c = g.addNode(new Object(), "c");
        g.addEdge(a, b, "cfg");
        g.addEdge(a, c, "cfg");
        g.addEdge(b, c, "cfg");
        assertThat(g.Serialize(), is(GS));
    }

    @Test
    public void load_graph() {
        Graph g = new Graph(GS);
        assertThat(g.Serialize(), is(GS));
    }
}

