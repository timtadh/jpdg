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


func (self *Graph) Slice(label string) (slices []*Graph) {
    obj, err := self.index.Get([]byte(label))
    if err != nil {
        panic(err)
    }
    matches := obj.([]*Vertex)
    for _, match := range matches {
        slices = append(slices, self.slice(match))
    }
    return slices
}

func (self *Graph) slice(start *Vertex) *Graph {
    g := newGraph()

    seen := make(map[int64]bool)
    var visit func(*Vertex)
    visit = func(n *Vertex) {
        seen[n.Id] = true
        g.addVertex(n)
        for _, parent := range self.parents[n.Id] {
            if !seen[parent] {
                visit(self.V[parent])
            }
        }
    }
    visit(start)

    for u := range g.V {
        for v := range g.V {
            if e, has := self.E[Arc{u,v}]; has {
                g.addEdge(e)
            }
        }
    }

    return g
}
