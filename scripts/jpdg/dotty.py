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


import os, sys, time, subprocess
import csv, json, cgi


def dotty(inf, of, html=True, label='label'):

    def string(label):
        if html:
            label = (
                label.replace("'", "\\'").
                replace('"', '\\"').
                replace('\n', '\\n')
            )
            label = cgi.escape(label).encode('ascii', 'xmlcharrefreplace')
            s = ''.join(
                '<tr><td align="left">' + line + "</td></tr>"
                for line in label.split('\\n')
            )
        else:
            s = (
                label.replace('"', '').
                replace('"', '').
                replace('\\', '').
                replace('\n', '')
            )
        return s
    def edge(line):
        of.write('n%d->n%d [label="%s"];\n' % (line['src'], line['targ'],
        line['label']))
    def vertex(line):
        if html:
            template = 'n%d [shape=rect, fontname="Courier", label=<<table border="0">%s</table>>];\n'
            of.write(template % (line['id'], string(line[label])))
        else:
            of.write('n%d [shape=rect, label="%s"];\n' % (line['id'], string(line[label])))
    def finalize():
        of.write('\n}\n')
    def start(graph_count):
        of.write('digraph G%d {\n' % graph_count)

    finalized = True
    graph_count = 0
    for i, line in enumerate(inf):
        if finalized:
            graph_count += 1
            start(graph_count)
        if '\t' not in line:
            finalize()
            finalized = True
            continue
        finalized = False
        lt, data = line.split('\t', 1)
        try:
            data = json.loads(data)
        except:
            print >>sys.stderr, data
            raise
        if i % 100 == 0: print >>sys.stderr, i, lt
        if lt == 'edge':
            edge(data)
        elif lt == 'vertex':
            vertex(data)
        elif lt == 'labels':
            pass
        else:
            raise Exception, "Unexpected line time %s" % lt
    if not finalized:
        finalize()

