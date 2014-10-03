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
    @Test
    public void testHello() {
        assertEquals("hello", "hello");
    }
/*
    String GS =
      "vertex	{\"id\":0,\"label\":\"a\",\"package_name\":\"x.y\",\"class_name\":\"c\",\"method_name\":\"m\",\"type\":\"t\",\"start_line\":1,\"start_column\":-1,\"end_line\":2,\"end_column\":-1}\n" +
      "vertex	{\"id\":1,\"label\":\"b\",\"package_name\":\"x.y\",\"class_name\":\"c\",\"method_name\":\"m\",\"type\":\"t\",\"start_line\":1,\"start_column\":-1,\"end_line\":2,\"end_column\":-1}\n" +
      "vertex	{\"id\":2,\"label\":\"c\",\"package_name\":\"x.y\",\"class_name\":\"c\",\"method_name\":\"m\",\"type\":\"t\",\"start_line\":1,\"start_column\":-1,\"end_line\":2,\"end_column\":-1}\n" +
      "edge	{\"src\":0,\"targ\":1,\"label\":\"cfg\",\"src_label\":\"a\",\"targ_label\":\"b\"}\n" + 
      "edge	{\"src\":0,\"targ\":2,\"label\":\"cfg\",\"src_label\":\"a\",\"targ_label\":\"c\"}\n" + 
      "edge	{\"src\":1,\"targ\":2,\"label\":\"cfg\",\"src_label\":\"b\",\"targ_label\":\"c\"}\n";
      // "labels	[\"a\",\"b\",\"c\"]\n";
    String E = "edge	{\"src\":0,\"targ\":1,\"label\":\"cfg\",\"src_label\":\"a\",\"targ_label\":\"b\"}";
    String V = "vertex	{\"id\":0,\"label\":\"a\",\"package_name\":\"x.y\",\"class_name\":\"c\",\"method_name\":\"m\",\"type\":\"t\",\"start_line\":2,\"start_column\":3,\"end_line\":4,\"end_column\":5}";

    @Test
    public void build_graph() {
        Graph g = new Graph();
        int a = g.addNode("a", "x.y", "c", "m", "t", 1, -1, 2, -1);
        int b = g.addNode("b", "x.y", "c", "m", "t", 1, -1, 2, -1);
        int c = g.addNode("c", "x.y", "c", "m", "t", 1, -1, 2, -1);
        g.addEdge(a, b, "cfg");
        g.addEdge(a, c, "cfg");
        g.addEdge(b, c, "cfg");
        assertThat(g.Serialize(), is(GS));
    }

    @Test
    public void Edge() {
        Graph g = new Graph();
        int a = g.addNode("a", "x.y", "c", "m", "t", 1, -1, 2, -1);
        int b = g.addNode("b", "x.y", "c", "m", "t", 1, -1, 2, -1);
        int c = g.addNode("c", "x.y", "c", "m", "t", 1, -1, 2, -1);
        Edge e = new Edge(a, b, "cfg", g);
        assertThat(e.Serialize(), is(E));
    }

    @Test
    public void Vertex() {
        Graph g = new Graph();
        g.addNode("a", "x.y", "c", "m", "t", 1, -1, 2, -1);
        Node a = new Node(0, 0, "x.y", "c", "m", "t", 2, 3, 4, 5, g);
        assertThat(a.Serialize(), is(V));
    }
    */
}

