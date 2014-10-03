package graph

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

import (
  "fmt"
  // "os"
  // "strings"
)
// import "github.com/timtadh/data-structures/types"

type Direction func(*Graph, *Vertex, map[string]bool, map[string]bool) *Graph

func (self *Graph) Slice(prefix string, dir Direction, filtered_edges, filtered_nodes map[string]bool) (slices []*Graph) {
    next := self.Index.PrefixFind([]byte(prefix))
    for _, obj, next := next(); next != nil; _, obj, next = next() {
        matches := obj.([]*Vertex)
        // fmt.Fprintln(os.Stderr, string(label.(types.ByteSlice)), len(matches))
        for _, match := range matches {
            graph := dir(self, match, filtered_edges, filtered_nodes)
            slices = append(slices, graph)
        }
    }
    return slices
}

func (self *Graph) SubGraph(nodes []int64, filtered_edges, filtered_nodes map[string]bool) *Graph {
    g := newGraph()
    for _, nid := range nodes {
        v := self.V[nid]
        g.addVertex(v)
    }
    add_edges(self, g, filtered_edges)
    return g
}

func (self *Graph) Partition(attr string, filtered_edges, filtered_nodes map[string]bool) (partition []*Graph, err error) {
    parts := make(map[string]*Graph)
    for _, v := range self.V {
        if _, has := v.Rest[attr]; !has {
            return nil, fmt.Errorf("attr not on node")
        }
        value := v.Rest[attr].(string)
        g, has := parts[value]
        if !has {
            g = newGraph()
            parts[value] = g
        } else {
            g.addVertex(v)
        }
    }
    for _, g := range parts {
        add_edges(self, g, filtered_edges)
        partition = append(partition, g)
    }
    return partition, nil
}

func (self *Graph) SelectAndConnect(prefix string) (proj *Graph) {
    proj = newGraph()
    next := self.Index.PrefixFind([]byte(prefix))
    for _, obj, next := next(); next != nil; _, obj, next = next() {
        matches := obj.([]*Vertex)
        for _, v := range matches {
            proj.addVertex(v)
        }
    }
    transitively_connect(self, proj)
    return proj
}

func (self *Graph) Reaches(u, v int64) bool {
    if self.closure == nil {
        self.compute_closure()
    }
    return self.closure[Arc{u,v}]
}

func (self *Graph) compute_closure() {
    dists := make([][]int, len(self.V))
    V := make([]int64, len(self.V))
    self.closure = make(map[Arc]bool)
    max_int := int((^uint(0)) >> 1)
    var i int = 0
    for u := range self.V {
        V[i] = u
        i++
    }
    for i := range dists {
        dists[i] = make([]int, len(self.V))
        for j := range dists[i] {
            if i != j {
                u := V[i]
                v := V[j]
                if _, has := self.E[Arc{u,v}]; has {
                    dists[i][j] = 1
                } else {
                    dists[i][j] = max_int
                }
            } else {
                dists[i][j] = 0
            }
        }
    }
    for k := 0; k < len(V); k++ {
        for i := 0; i < len(V); i++ {
            for j := 0; j < len(V); j++ {
                if dists[i][j] > dists[i][k] + dists[k][j] && dists[i][k] + dists[k][j] > 0 {
                    dists[i][j] = dists[i][k] + dists[k][j]
                }
            }
        }
    }
    for i := range dists {
        for j := range dists {
            if i != j && dists[i][j] < max_int {
                u := V[i]
                v := V[j]
                self.closure[Arc{u,v}] = true
            }
        }
    }
}


func Both(self *Graph, start *Vertex, filtered_edges, filtered_nodes map[string]bool) *Graph {
    g := newGraph()
    forward(self, g, start, filtered_edges, filtered_nodes)
    backward(self, g, start, filtered_edges, filtered_nodes)
    return g
}

func Backward(self *Graph, start *Vertex, filtered_edges, filtered_nodes map[string]bool) *Graph {
    g := newGraph()
    backward(self, g, start, filtered_edges, filtered_nodes)
    return g
}

func add_edges(self, g *Graph, filtered_edges map[string]bool) {
    for u := range g.V {
        for v := range g.V {
            if edges, has := self.E[Arc{u,v}]; has {
                for _, e := range edges {
                    _, skip := filtered_edges[e.Label]
                    if !skip {
                        g.addEdge(e)
                    }
                }
            }
        }
    }
}

func transitively_connect(self, g *Graph) {
    for u := range g.V {
        for v := range g.V {
            if self.Reaches(u, v) {
                g.addEdge(&Edge{Arc{u,v}, "", make(jsonObject)})
            }
        }
    }
}

func backward(self, g *Graph, start *Vertex, filtered_edges, filtered_nodes map[string]bool) {
    seen := make(map[int64]bool)
    var visit func(*Vertex)
    visit = func(n *Vertex) {
        seen[n.Id] = true
        /*if strings.HasSuffix(n.Label, "-entry") {
            return
        }*/
        g.addVertex(n)
        for _, parent := range self.parents[n.Id] {
            _, skip_node := filtered_nodes[self.V[parent].Label]
            skip_edge := has_only_filtered_edges(self, Arc{parent,n.Id}, filtered_edges)
            if !seen[parent] && !(skip_node || skip_edge) {
                visit(self.V[parent])
            }
        }
    }
    visit(start)
    add_edges(self, g, filtered_edges)
}

func Forward(self *Graph, start *Vertex, filtered_edges, filtered_nodes map[string]bool) *Graph {
    g := newGraph()
    forward(self, g, start, filtered_edges, filtered_nodes)
    return g
}

func forward(self, g *Graph, start *Vertex, filtered_edges, filtered_nodes map[string]bool) {
    seen := make(map[int64]bool)
    var visit func(*Vertex)
    visit = func(n *Vertex) {
        seen[n.Id] = true
        /*if strings.HasSuffix(n.Label, "-entry") {
            return
        }*/
        g.addVertex(n)
        for _, kid := range self.kids[n.Id] {
            _, skip_node := filtered_nodes[self.V[kid].Label]
            skip_edge := has_only_filtered_edges(self, Arc{n.Id,kid}, filtered_edges)
            if !seen[kid] && !skip_node && !skip_edge {
                visit(self.V[kid])
            }
        }
    }
    visit(start)
    add_edges(self, g, filtered_edges)
}

func has_only_filtered_edges(self *Graph, arc Arc, filtered_edges map[string]bool) bool {
    for _, e := range self.E[arc] {
        if _, skip := filtered_edges[e.Label]; !skip {
            return false
        }
    }
    return true
}


