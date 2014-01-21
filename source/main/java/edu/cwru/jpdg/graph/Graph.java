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
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

/**
 * Represents a multi edged graph. Supports serialization and de-serialization
 */
public class Graph<T> {

    int next_uid = 0;
    ConcurrentHashMap<T,Integer> nodes = new ConcurrentHashMap<T,Integer>();
    ConcurrentHashMap<Integer,String> node_labels =
        new ConcurrentHashMap<Integer,String>();
    ConcurrentHashMap<String,ConcurrentHashMap<Integer,Set<Integer>>> edges =
        new ConcurrentHashMap<String,ConcurrentHashMap<Integer,Set<Integer>>>();

    public Graph() { }

    public Graph(String serialized) {
        Map<String, Object> json = (new Gson()).fromJson(serialized, Map.class);
        List<String> labels = ((List<String>)json.get("node_labels"));
        for (int i = 0; i < labels.size(); i++) {
            node_labels.put(i, labels.get(i));
        }
        Map<String,List<List<Double>>> _edges = ((Map<String,List<List<Double>>>)json.get("edges"));
        for (Map.Entry<String,List<List<Double>>> e : _edges.entrySet()) {
            String name = e.getKey();
            for (int i = 0; i < labels.size(); i++) {
                for (double j : e.getValue().get(i)) {
                    this.addEdge(i, (int)j, name);
                }
            }
        }
        next_uid = labels.size();
    }

    public int addNode(T node) {
        return addNode(node, "");
    }

    public synchronized int addNode(T node, String label) {
        if (nodes.containsKey(node)) {
            return nodes.get(node);
        }
        int uid = next_uid;
        next_uid++;
        nodes.put(node, uid);
        node_labels.put(uid, label);
        return uid;
    }

    public int nodeUID(T node) {
        return addNode(node);
    }

    public String label(int node) throws NoSuchElementException {
        if (!node_labels.containsKey(node)) {
            throw new NoSuchElementException("uid has not yet been created");
        }
        return node_labels.get(node);
    }

    public void setLabel(int node, String label) throws NoSuchElementException {
        if (!node_labels.containsKey(node)) {
            throw new NoSuchElementException("uid has not yet been created");
        }
        node_labels.put(node, label);
    }

    public void addEdge(int a, int b, String type) throws NoSuchElementException {
        if (!node_labels.containsKey(a)) {
            throw new NoSuchElementException("uid a has not yet been created");
        }
        if (!node_labels.containsKey(b)) {
            throw new NoSuchElementException("uid b has not yet been created");
        }
        edges.putIfAbsent(type, new ConcurrentHashMap<Integer,Set<Integer>>());
        edges.get(type).putIfAbsent(a, Collections.synchronizedSet(new HashSet<Integer>()));
        edges.get(type).get(a).add(b);
    }

    public synchronized String Serialize() {
        Map<String,Object> M = new HashMap<String,Object>();
        M.put("node_labels", new ArrayList<String>());
        for (int i = 0; i < next_uid; i++) {
            String label = node_labels.get(i);
            if (label == null) {
                label = "";
            }
            ((List<String>)M.get("node_labels")).add(i, label);
        }
        M.put("edges", new HashMap<String,List<List<Integer>>>());
        for (Map.Entry<String,ConcurrentHashMap<Integer,Set<Integer>>> e : edges.entrySet()) {
            List<List<Integer>> edges = new ArrayList<List<Integer>>();
            ((Map)M.get("edges")).put(e.getKey(), edges);
            for (int i = 0; i < next_uid; i++) {
                List<Integer> e_list = new ArrayList<Integer>();
                Set<Integer> e_set = e.getValue().get(i);
                if (e_set != null) {
                    for (int j : e_set) {
                        e_list.add(j);
                    }
                }
                edges.add(i, e_list);
            }
        }
        return (new Gson()).toJson(M); // to do, figure out how to actually do this.
    }
}
