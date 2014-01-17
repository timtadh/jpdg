package edu.cwru.jpdg.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.cedarsoftware.util.io.*;

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

    public Graph() {
    }

    public Graph(String serialized) {
    }

    public int addNode(T node) {
        if (nodes.containsKey(node)) {
            return nodes.get(node);
        }
        int uid;
        synchronized (this) {
            uid = next_uid;
            next_uid++;
        }
        nodes.put(node, uid);
        node_labels.put(uid, "");
        return uid;
    }

    public int nodeUID(T node) {
        return addNode(node);
    }

    public void addEdge(int a, int b, String type) throws NoSuchElementException {
        if (!node_labels.containsKey(a)) {
            throw new NoSuchElementException("uid a did has not yet been created");
        }
        if (!node_labels.containsKey(b)) {
            throw new NoSuchElementException("uid b did has not yet been created");
        }
        edges.putIfAbsent(type, new ConcurrentHashMap<Integer,Set<Integer>>());
        edges.get(type).putIfAbsent(a, Collections.synchronizedSet(new HashSet<Integer>()));
        edges.get(type).get(a).add(b);
    }

    public String Serialize() throws java.io.IOException {
        Map<String,Object> M = new HashMap<String,Object>();
        M.put("node_labels", node_labels);
        M.put("edges", edges);
        return JsonWriter.objectToJson(M); // to do, figure out how to actually do this.
    }
}
