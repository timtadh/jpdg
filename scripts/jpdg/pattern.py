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


from dot_tools import parse

from jpdg.graph import Graph


class Pattern(object):

    def __init__(self, pattern, examples, ex_maps, pat_file, slice_file, label):
        self.pattern = pattern
        self.examples = examples
        self.ex_maps = ex_maps
        self.pattern_file = pat_file
        self.slice_file = slice_file
        self.label = label

    def dotty(self):
        header = 'digraph {'
        footer = '}'
        s = self.pattern.dotty('Pattern', True, True)
        for i, e in enumerate(self.examples[:10]):
            s += '\n'*2 + e.dotty('E%d' % i, True, True)
        s += '\n'
        return header + '\n' + s + footer


def build_patterns(pattern_file, slice_file, label, slicer):

    def get_slices():
        with open(slice_file) as f:
            sast = parse(f.read())
        slices = dict()
        for gast in sast.children:
            sg = Graph.build('dot', gast)
            slices[gast.children[1].label] = sg
        return slices

    def get_subgraphs(slices):
        with open(pattern_file) as f:
            past = parse(f.read())

        subgraphs = list()
        for i in xrange(0, len(past.children), 2):
            gast = past.children[i]
            comment = past.children[i+1].children[0].label
            sg = Graph.build('dot', gast)
            labels = set(label for label in sg.nodes.itervalues())
            if ((label is None and
                  any(l.startswith('3:call') and 'Iterator' not in l
                      for l in labels))
              or label in labels):
                subgraphs.append((sg,
                    [slices[e] for e in parse_examples(comment)]))
        return subgraphs

    def map_subgraphs(subgraphs):
        maps = list()
        for pattern, examples in subgraphs:
            ex_maps = list()
            for ex in examples:
                for m in match(label, pattern, ex):
                    pdg_map = dict()
                    for pn, sn in m.iteritems():
                        sn = int(sn.replace('n', ''))
                        pdg_map[pn] = slicer.node(sn)
                    ex_maps.append(pdg_map)
            maps.append((pattern, ex_maps))
        return maps

    maps = map_subgraphs(get_subgraphs(get_slices()))
    mapped_patterns = list()

    for pattern, ex_maps in maps:
        examples = [
            Graph.build('veg', slicer.sub_graph(
                [v['id'] for v in ex_map.values()],
                filtered_edges=['cfg']
            ), label='extra')
            for ex_map in ex_maps
        ]
        mapped_patterns.append(
            Pattern(pattern, examples, ex_maps, pattern_file, slice_file, label))

    return mapped_patterns

def parse_examples(comment):
    comment = comment.strip()
    s = comment[comment.index('[')+1:-1].replace(' ', '')
    return [
        e
        for e in s.split(',')
        if e
    ]

def match(center, pattern, graph):
    def match(u, v):
        pgmap = dict()
        seen_u = set()
        seen_v = set()
        def visit(a, b):
            seen_u.add(a)
            seen_v.add(b)
            for akid in pattern.kids(a):
                if akid in seen_u:
                    continue
                for bkid in graph.kids(b):
                    if pattern.nodes[akid] == graph.nodes[bkid]:
                        if bkid not in seen_v:
                            visit(akid, bkid)
                        break
                else:
                    # unmatched kid
                    return
            for apar in pattern.parents(a):
                if apar in seen_u:
                    continue
                for bpar in graph.parents(b):
                    if pattern.nodes[apar] == graph.nodes[bpar]:
                        if bpar not in seen_v:
                            visit(apar, bpar)
                        break
                else:
                    return
            pgmap[a] = b

        visit(u,v)
        if set(pgmap.keys()) != set(pattern.nodes.keys()):
            return None
        return pgmap

    def centered_match(center):
        matches = list()
        for u in pattern.index[center]:
            for v in graph.index[center]:
                m = match(u, v)
                if m is not None:
                    matches.append(m)
        return matches

    def uncentered_match():
        matches = list()
        seen_v = set()
        for u, label in pattern.nodes.iteritems():
            for v in graph.index[label]:
                m = match(u, v)
                if m is None:
                    continue
                if any(b in seen_v for b in m.itervalues()):
                    continue
                for b in m.itervalues():
                    seen_v.add(b)
                matches.append(m)
        return matches

    if center is not None:
        return centered_match(center)
    return uncentered_match()

