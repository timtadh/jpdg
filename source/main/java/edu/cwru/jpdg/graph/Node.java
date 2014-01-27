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

public class Node {

    public int id;
    public int label;
    public String package_name;
    public String class_name;
    public String method_name;
    public int start_line = -1;
    public int start_column = -1;
    public int end_line = -1;
    public int end_column = -1;
    Graph g;

    public Node(int id, int label,
                String package_name, String class_name, String method_name,
                int start_line, int start_column, int end_line, int end_column, Graph g) {
        this.id = id;
        this.package_name = package_name;
        this.class_name = class_name;
        this.method_name = method_name;
        this.label = label;
        this.start_line = start_line;
        this.start_column = start_column;
        this.end_line = end_line;
        this.end_column = end_column;
        this.g = g;
    }

    public String Serialize() {
        Map<String,Object> M = new LinkedHashMap<String,Object>();
        M.put("id", id);
        M.put("label", g.label(id));
        M.put("package_name", package_name);
        M.put("class_name", class_name);
        M.put("method_name", method_name);
        M.put("start_line", start_line);
        M.put("start_column", start_column);
        M.put("end_line", end_line);
        M.put("end_column", end_column);
        return "vertex\t" + (new Gson()).toJson(M);
    }
}

