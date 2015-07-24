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

    public final int id;
    public final int lnum;
    public final String label;
    public final String extra;
    public final String package_name;
    public final String class_name;
    public final String source_file;
    public final String method_name;
    public final String type;
    public final int start_line;
    public final int start_column;
    public final int end_line;
    public final int end_column;

    public Node(int id, int lnum, String label, String extra,
                String package_name, String class_name, String source_file,
                String method_name, String type,
                int start_line, int start_column, int end_line, int end_column) {
        this.id = id;
        this.lnum = lnum;
        this.label = label;
        this.extra = extra;
        this.package_name = package_name;
        this.class_name = class_name;
        this.source_file = source_file;
        this.method_name = method_name;
        this.type = type;
        this.start_line = start_line;
        this.start_column = start_column;
        this.end_line = end_line;
        this.end_column = end_column;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Node) {
            return equals((Node)o);
        }
        return false;
    }

    public boolean equals(Node n) {
        if (n == null) {
            return false;
        }
        return n.label.equals(label) &&
               n.package_name.equals(package_name) &&
               n.class_name.equals(class_name) &&
               n.source_file.equals(source_file) &&
               n.method_name.equals(method_name) &&
               n.type.equals(type) &&
               n.start_line == start_line &&
               n.start_column == start_column &&
               n.end_line == end_line &&
               n.end_column == end_column;
    }

    @Override
    public int hashCode() {
        int code = 43;
        code = label == null ? code : 3*code + label.hashCode();
        code = package_name == null ? code : 7*code + package_name.hashCode();
        code = class_name == null ? code : 11*code + class_name.hashCode();
        code = source_file == null ? code : 13*code + source_file.hashCode();
        code = method_name == null ? code : 17*code + method_name.hashCode();
        code = type == null ? code : 23*code + type.hashCode();
        code = 29*code + start_line;
        code = 31*code + start_column;
        code = 37*code + end_line;
        code = 39*code + end_column;
        return code;
    }

    public String Serialize() {
        Map<String,Object> M = new LinkedHashMap<String,Object>();
        M.put("id", id);
        M.put("label", label);
        M.put("extra", extra);
        M.put("package_name", package_name);
        M.put("class_name", class_name);
        M.put("source_file", source_file);
        M.put("method_name", method_name);
        M.put("type", type);
        M.put("start_line", start_line);
        M.put("start_column", start_column);
        M.put("end_line", end_line);
        M.put("end_column", end_column);
        return "vertex\t" + (new Gson()).toJson(M);
    }
}

