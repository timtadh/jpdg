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

import sys, os, threading, time, subprocess
from collections import deque

class Slicer(object):

    def __init__(self, debug=False):
        self.debug = debug
        self.p = subprocess.Popen(['slicebot'],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE)
        self.slicer_lock = threading.Lock()
        self.lines = deque()
        self.lines_cv = threading.Condition()
        self.closed = False
        self.read_thread = threading.Thread(target=self.listen)
        self.read_thread.daemon = True
        self.read_thread.start()

    def close(self):
        self._close(False)

    def _close(self, from_read=False):
        if self.debug and from_read:
            print >>sys.stderr, "read thread closing it"
        with self.slicer_lock:
            with self.lines_cv:
                if self.closed:
                    if self.debug and from_read:
                        print >>sys.stderr, "read thread bailed"
                    return
                self.closed = True
                self.lines_cv.notifyAll()
        self.p.kill()
        if not from_read:
            self.read_thread.join()
        if self.debug:
            print >>sys.stderr, "closed"
            if from_read:
                print >>sys.stderr, "read thread closed it"

    def load(self, path):
        return self.command('LOAD', path, self.generic_response)

    def generic_response(self):
        cmd, data = self.get_line()
        if cmd == "ERROR":
            raise Exception(data)
        elif cmd != "OK":
            raise Exception, "bad command recieved %s %s" % (cmd, data)
        else:
            return True

    def candidates(self, prefix):
        return self.command('CANDIDATES', prefix, self.candidates_response)

    def candidates_response(self):
        cmd, data = self.get_line()
        if cmd == "ERROR":
            raise Exception(data)
        elif cmd != "CANDIDATES":
            raise Exception, "bad command recieved %s %s" % (cmd, data)
        else:
            lines = [
                line.split(', ', 1)
                for line in data.strip().split('\n')
                if line and ', ' in line
            ]
            return [
                {'label': row[1], 'count': int(row[0])}
                for row in lines
            ]

    def slice(self, prefix, direction=None, filtered_edges=None):
        args = ['-p', prefix]
        if direction is not None:
            args.append('-d')
            args.append(direction)
        if filtered_edges is not None:
            for e in filtered_edges:
                args.append('-e')
                args.append(e)
        return self.command('SLICE', ' '.join(args), self.slice_response)

    def slice_response(self):
        cmd, data = self.get_line()
        if cmd == "ERROR":
            raise Exception(data)
        elif cmd != "GRAPHS":
            raise Exception, "bad command recieved %s %s" % (cmd, data)
        else:
            lines = [
                line
                for line in data.strip().split('\n')
            ]
            graphs = list()
            graph = list()
            graphs.append(graph)
            for line in lines:
                if not line:
                    graph = list()
                    graphs.append(graph)
                else:
                    graph.append(line)
            return [
                graph
                for graph in graphs
                if graph
            ]

    def command(self, cmd, data, response):
        with self.slicer_lock:
            msg = cmd + " " + data.encode('base64').replace('\n', '') + '\n'
            self.p.stdin.write(msg)
            return response()

    def listen(self):
        chunk = ''
        while True:
            while "\n" not in chunk:
                try:
                    data = self.p.stdout.read(1)
                    if not data:
                        self._close(True)
                        return
                    chunk += data
                except socket.timeout, t:
                    ## timeout retry
                    pass
                except Exception, e:
                    print >>sys.stderr, e, type(e)
                    self._close(True)
                    return
            line, chunk = chunk.split('\n', 1)
            with self.lines_cv:
                self.lines.append(line)
                self.lines_cv.notify()

    def get_line(self):
        with self.lines_cv:
            while len(self.lines) <= 0:
                if self.closed:
                    raise Exception, "queued connection closed"
                self.lines_cv.wait()
            line = self.lines.popleft()
        return self.process_line(line)

    def process_line(self, line):
        split = line.split(' ', 1)
        command = split[0]
        rest = None
        if len(split) > 1:
            rest = split[1].decode('base64')

        return command, rest

def _loop(slicer):
    while True:
        try:
            line = raw_input('> ')
        except:
            break
        split = line.split(' ', 1)
        command = split[0]
        data = None
        try:
            if len(split) > 1:
                data = split[1]
            if command == 'load' and data is not None:
                print slicer.load(data)
            elif command == 'candidates' and data is not None:
                for c in slicer.candidates(data):
                    print c
            elif command == 'slice' and data is not None:
                for c in slicer.slice(data, filtered_edges=['ddg', 'cdg']):
                    for line in c:
                        print line
                    print
            else:
                print slicer.command(command.upper(), data if data is not None else
                        '', slicer.generic_response)
        except Exception, e:
            print >>sys.stderr, type(e), e
        print

def main():
    slicer = Slicer()
    try:
       _loop(slicer)
    finally:
        slicer.close()

if __name__ == '__main__':
    main()

