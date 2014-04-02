#!/usr/bin/env python
# -*- coding: utf-8 -*-

# Tim Henderson (tadh@case.edu)
#
# This file is part of jpdg a library to generate Program Dependence Graphs
# from JVM bytecode.
#
# Copyright (c) 2014, Tim Henderson, Case Western Reserve University
#   Cleveland, Ohio 44106
#   All Rights Reserved.
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc.,
#   51 Franklin Street, Fifth Floor,
#   Boston, MA  02110-1301
#   USA
# or retrieve version 2.1 at their website:
#   http://www.gnu.org/licenses/lgpl-2.1.html


import os, sys, json
from dot_tools.dot_graph import SimpleGraph


class Graph(SimpleGraph):

    def kids(self, s):
        return self.eindex.get(s, list())

    def parents(self, t):
        return self.reindex.get(t, list())

    @staticmethod
    def build(type, *args):
        self = None
        if type == 'dot':
            self = Graph()
            sg = SimpleGraph.build(*args)
            self.nodes = sg.nodes
            self.edges = sg.edges
            self.index = sg.index
        elif type == 'veg':
            self = Graph.build_veg(*args)
        else:
            raise Exception, "Unknown graph type"

        self.eindex = dict()
        self.reindex = dict()
        for s, t, _ in self.edges:
            self.eindex[s] = targs = self.eindex.get(s, list())
            targs.append(t)
            self.reindex[t] = srcs = self.reindex.get(t, list())
            srcs.append(s)

        return self

    @staticmethod
    def build_veg(inf):
        self = Graph()
        self.node_lines = dict()

        def edge(line):
            src = line['src']
            targ = line['targ']
            label = line['label']
            self.edges.append((src, targ, label))

        def vertex(line):
            label = line['label']
            self.nodes[line['id']] = label
            self.node_lines[line['id']] = line
            self.index[label] = nodes = self.index.get(label, list())
            nodes.append(line['id'])

        for i, line in enumerate(inf):
            lt, data = line.split('\t', 1)
            try:
                data = json.loads(data)
            except:
                print >>sys.stderr, data
                raise

            if lt == 'edge':
                edge(data)
            elif lt == 'vertex':
                vertex(data)
            else:
                raise Exception, "Unexpected line time %s" % lt

        return self

