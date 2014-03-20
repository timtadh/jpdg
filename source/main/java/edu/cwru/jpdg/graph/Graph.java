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
import java.lang.StringBuilder;

import com.google.gson.Gson;

/**
 * Represents a multi edged graph. Supports serialization and de-serialization
 */
public class Graph {

    int next_uid = 0;

    HashMap<Integer,Node> nodes = new HashMap<Integer,Node>();
    List<String> labels =
        new ArrayList<String>();
    HashMap<String,Integer> rlabels =
        new HashMap<String,Integer>();
    HashMap<Integer,Integer> node_labels =
        new HashMap<Integer,Integer>();
    HashMap<String,HashMap<Integer,Set<Integer>>> edges =
        new HashMap<String,HashMap<Integer,Set<Integer>>>();

    public Graph() { }

    public int addNode(String label, String extra, String package_name, String class_name, String method_name, String type, int start_l, int start_c, int end_l, int end_c) {
        int uid = next_uid;
        next_uid++;
        nodes.put(uid, new Node(uid, label_num(label), extra, package_name, class_name, method_name, type, start_l, start_c, end_l, end_c, this));
        node_labels.put(uid, label_num(label));
        return uid;
    }

    private int label_num(String label) {
        if (rlabels.containsKey(label)) {
            return rlabels.get(label);
        }
        int num = labels.size();
        rlabels.put(label, num);
        labels.add(label);
        return num;
    }

    public int labelNum(int node) {
        if (!node_labels.containsKey(node)) {
            throw new NoSuchElementException("uid has not yet been created");
        }
        return node_labels.get(node);
    }

    public String label(int node) throws NoSuchElementException {
        if (!node_labels.containsKey(node)) {
            throw new NoSuchElementException("uid has not yet been created");
        }
        return labels.get(node_labels.get(node));
    }

    public void setLabel(int node, String label) throws NoSuchElementException {
        if (!node_labels.containsKey(node)) {
            throw new NoSuchElementException("uid has not yet been created");
        }
        node_labels.put(node, label_num(label));
    }

    public void addEdge(int a, int b, String type) throws NoSuchElementException {
        if (!node_labels.containsKey(a)) {
            throw new NoSuchElementException("uid a has not yet been created");
        }
        if (!node_labels.containsKey(b)) {
            throw new NoSuchElementException("uid b has not yet been created");
        }
        if (!edges.containsKey(type)) {
            edges.put(type, new HashMap<Integer,Set<Integer>>());
        }
        if (!edges.get(type).containsKey(a)) {
            edges.get(type).put(a, new HashSet<Integer>());
        }
        edges.get(type).get(a).add(b);
    }

    public boolean hasEdge(int a, int b, String type) {
        if (!edges.containsKey(type)) {
            return false;
        }
        if (!edges.get(type).containsKey(a)) {
            return false;
        }
        return edges.get(type).get(a).contains(b);
    }

    public String Serialize() {
        StringBuilder sb = new StringBuilder();
        for (Node n : nodes.values()) {
            sb.append(n.Serialize());
            sb.append("\n");
        }
        for (Map.Entry<String,HashMap<Integer,Set<Integer>>> E : edges.entrySet()) {
            String e_label = E.getKey();
            HashMap<Integer,Set<Integer>> edges = E.getValue();
            for (Map.Entry<Integer,Set<Integer>> e : edges.entrySet()) {
                int i = e.getKey();
                for (int j : e.getValue()) {
                    sb.append((new Edge(i, j, e_label, this)).Serialize());
                    sb.append("\n");
                }
            }
        }
        //sb.append("labels\t");
        //sb.append((new Gson()).toJson(labels));
        //sb.append("\n");
        return sb.toString();
    }
}
