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
  "strings"
)
// import "github.com/timtadh/data-structures/types"

type Direction func(*Graph, *Vertex, map[string]bool) *Graph

func (self *Graph) Slice(prefix string, dir Direction, filtered_edges map[string]bool) (slices []*Graph) {
    next := self.Index.PrefixFind([]byte(prefix))
    for _, obj, next := next(); next != nil; _, obj, next = next() {
        matches := obj.([]*Vertex)
        // fmt.Fprintln(os.Stderr, string(label.(types.ByteSlice)), len(matches))
        for _, match := range matches {
            graph := dir(self, match, filtered_edges)
            slices = append(slices, graph)
        }
    }
    return slices
}

func (self *Graph) SubGraph(nodes []int64, filtered_edges map[string]bool) *Graph {
    g := newGraph()
    for _, nid := range nodes {
        v := self.V[nid]
        g.addVertex(v)
    }
    add_edges(self, g, filtered_edges)
    return g
}

func Both(self *Graph, start *Vertex, filtered_edges map[string]bool) *Graph {
    g := newGraph()
    forward(self, g, start, filtered_edges)
    backward(self, g, start, filtered_edges)
    return g
}

func Backward(self *Graph, start *Vertex, filtered_edges map[string]bool) *Graph {
    g := newGraph()
    backward(self, g, start, filtered_edges)
    return g
}

func add_edges(self, g *Graph, filtered_edges map[string]bool) {
    for u := range g.V {
        for v := range g.V {
            if e, has := self.E[Arc{u,v}]; has {
                _, skip := filtered_edges[e.Label]
                if !skip {
                    g.addEdge(e)
                }
            }
        }
    }
}

func backward(self, g *Graph, start *Vertex, filtered_edges map[string]bool) {
    seen := make(map[int64]bool)
    var visit func(*Vertex)
    visit = func(n *Vertex) {
        seen[n.Id] = true
        if strings.HasSuffix(n.Label, "-entry") {
            return
        }
        g.addVertex(n)
        for _, parent := range self.parents[n.Id] {
            _, skip := filtered_edges[self.E[Arc{parent,n.Id}].Label]
            if !seen[parent] && !skip {
                visit(self.V[parent])
            }
        }
    }
    visit(start)
    add_edges(self, g, filtered_edges)
}

func Forward(self *Graph, start *Vertex, filtered_edges map[string]bool) *Graph {
    g := newGraph()
    forward(self, g, start, filtered_edges)
    return g
}

func forward(self, g *Graph, start *Vertex, filtered_edges map[string]bool) {
    seen := make(map[int64]bool)
    var visit func(*Vertex)
    visit = func(n *Vertex) {
        seen[n.Id] = true
        if strings.HasSuffix(n.Label, "-entry") {
            return
        }
        g.addVertex(n)
        for _, kid := range self.kids[n.Id] {
            _, skip := filtered_edges[self.E[Arc{n.Id,kid}].Label]
            if !seen[kid] && !skip {
                visit(self.V[kid])
            }
        }
    }
    visit(start)
    add_edges(self, g, filtered_edges)
}

