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

import java.util.*;

import com.google.gson.Gson;

public class Edge {

    public int src;
    public int targ;
    public String label;
    Graph g;

    public Edge(int src, int targ, String label, Graph g) {
        this.src = src;
        this.targ = targ;
        this.label = label;
        this.g = g;
    }

    public String Serialize() {
        Map<String,Object> M = new LinkedHashMap<String,Object>();
        M.put("src", src);
        M.put("targ", targ);
        M.put("label", label);
        M.put("src_label", g.label(src));
        M.put("targ_label", g.label(targ));
        return "edge\t" + (new Gson()).toJson(M);
    }
}
