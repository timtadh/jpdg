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


import os, sys, time, subprocess, tempfile, shutil
import collections
try:
    from cStringIO import StringIO
except:
    from StringIO import StringIO

import optutils
from optutils import output, log, error_codes, add_code
from dot_tools import parse

from jpdg import config
from jpdg.slicer import Slicer
from jpdg.dotty import dotty
from jpdg.graph import Graph
from jpdg.pattern import build_patterns


def build_jpdg(conf):
    cwd = os.getcwd()
    os.chdir(conf.jpdg_root)
    try:
        subprocess.check_call([
            'buildr', 'jpdg:package', 'test=no'
        ])
    finally:
        os.chdir(cwd)

def build_parsemis(conf):
    cwd = os.getcwd()
    os.chdir(conf.jpdg_root)
    try:
        subprocess.check_call([
            'buildr', 'parsemis:package', 'test=no'
        ])
    finally:
        os.chdir(cwd)

def build_slicer(conf):
    subprocess.check_call([
        'go', 'install', 'cwru/selab/slice/slicebot',
    ])

def run_jpdg(conf, name, subject, output, no_build=False, jpdg_logs=False):
    if not no_build:
        build_jpdg(conf)
    cmd = list(subject.jpdg_cmd)
    cmd.append(output)
    print >>sys.stderr, '>', ' '.join(cmd)
    if not jpdg_logs:
        p = subprocess.Popen(cmd,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    else:
        p = subprocess.Popen(cmd)
    if p.wait() != 0:
        if not jpdg_logs:
            print >>sys.stderr, p.stdout.read()
            print >>sys.stderr, p.stderr.read()
        print >>sys.stderr, "Calling jpdg failed"
        print >>sys.stderr
        for arg in cmd[:-1]:
            print >>sys.stderr, arg, '\\'
        print >>sys.stderr, cmd[-1]
        sys.exit(error_codes['jpdg'])

def run_parsemis(conf, dotty_output, swap, parsemis_output, no_build=False,
        parsemis_logs=False, pattern_min='50%', extra=None):

    if extra is None:
        extra = list()

    if not no_build:
        build_parsemis(conf)

    cmd = list(conf.parsemis_cmd)
    cmd += [
        '--graphFile='+dotty_output,
        '--swapFile='+swap,
        '--outputFile='+parsemis_output,
        '--closeGraph=true',
        '--algorithm=gspan',
        '--minimumFrequency='+pattern_min,
        '--minimumNodeCount=3',
        '--minimumEdgeCount=3',
        '--maximumNodeCount=10',
    ]
    cmd += extra

    print >>sys.stderr, '>', ' '.join(cmd)
    if not parsemis_logs:
        p = subprocess.Popen(cmd,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    else:
        p = subprocess.Popen(cmd)
    if p.wait() != 0:
        if not parsemis_logs:
            print >>sys.stderr, p.stdout.read()
            print >>sys.stderr, p.stderr.read()
        print >>sys.stderr, "Calling parsemis failed"
        print >>sys.stderr
        for arg in cmd[:-1]:
            print >>sys.stderr, arg, '\\'
        print >>sys.stderr, cmd[-1]
        sys.exit(error_codes['parsemis'])

def run_graphviz(dot_file):
    cmd = [
        'sed', '-i', '/transparent/d', dot_file
    ]
    print >>sys.stderr, '>', ' '.join(cmd)
    subprocess.check_call(cmd)
    cmd = [
        'dot', '-Tpng', '-O', dot_file
    ]
    print >>sys.stderr, '>', ' '.join(cmd)
    subprocess.check_call(cmd)

def patterns(conf, name, subject, output, slicer,
             no_build=False, jpdg_logs=False, minimum=1, pattern_min='50%',
             parsemis_args=None,):

    if parsemis_args is None:
        parsemis_args = list()

    if not no_build:
        build_jpdg(conf)
        build_parsemis(conf)
    jpdg_output = os.path.join(output, 'graph.pdg')
    run_jpdg(conf, name, subject, jpdg_output, True, jpdg_logs)

    patterns = list()
    slicer.load(jpdg_output)
    for i, c in enumerate(slicer.candidates('3:call')):
        if c['count'] < minimum:
            continue
        slices = slicer.slice(c['label'], filtered_edges=['cfg'])
        memfile = StringIO(slices)
        dotty_output = os.path.join(output, 'slice.%d.dot' % i)
        with open(dotty_output, 'w') as f:
            dotty(memfile, f, html=False)
        if os.stat(dotty_output).st_size == 0:
            os.unlink(dotty_output)
            continue
        parsemis_output = os.path.join(output, "patterns.%d.dot" % i)
        swap_file = os.path.join(output, "parsemis_swap")
        run_parsemis(
            conf,
            dotty_output,
            swap_file,
            parsemis_output,
            no_build=True,
            pattern_min=pattern_min,
            extra=parsemis_args,
        )
        os.unlink(swap_file)
        if os.stat(parsemis_output).st_size == 0:
            os.unlink(parsemis_output)
        else:
            run_graphviz(parsemis_output)
            patterns += build_patterns(
                parsemis_output, dotty_output, c['label'], slicer)

    return patterns

def partition_patterns(conf, name, subject, output, slicer,
             no_build=False, jpdg_logs=False, minimum=1, pattern_min='50%',
             parsemis_args=None,):

    if parsemis_args is None:
        parsemis_args = list()

    if not no_build:
        build_jpdg(conf)
        build_parsemis(conf)
    jpdg_output = os.path.join(output, 'graph.pdg')
    run_jpdg(conf, name, subject, jpdg_output, True, jpdg_logs)

    slicer.load(jpdg_output)
    print 'loaded'
    partition = slicer.partition('method_name', filtered_edges=['cfg'])
    print 'parted'
    memfile = StringIO(partition)
    dotty_output = os.path.join(output, 'partition.dot')
    print 'dotty'
    with open(dotty_output, 'w') as f:
        dotty(memfile, f, html=False)
    if os.stat(dotty_output).st_size == 0:
        os.unlink(dotty_output)
        return list()

    print 'dottied'
    parsemis_output = os.path.join(output, "parsemis_patterns.dot")
    swap_file = os.path.join(output, "parsemis_swap")
    run_parsemis(
        conf,
        dotty_output,
        swap_file,
        parsemis_output,
        no_build=True,
        pattern_min=pattern_min,
        extra=parsemis_args,
    )
    os.unlink(swap_file)

    patterns = list()

    if os.stat(parsemis_output).st_size == 0:
        os.unlink(parsemis_output)
    else:
        run_graphviz(parsemis_output)
        patterns += build_patterns(
            parsemis_output, dotty_output, None, slicer)

    return patterns


def graphviz_patterns(patterns, output):
    for i, p in enumerate(patterns):
        p_file = os.path.join(output, 'final_pattern.%d.dot' % i)
        with open(p_file, 'w') as f:
            f.write(p.dotty())
        run_graphviz(p_file)

