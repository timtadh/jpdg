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

import os, sys, copy, glob

from optutils import conf

def shell_str(*args):
    if len(args) == 0:
        return ''
    return os.path.expandvars(''.join(args))

types = dict(conf.default_types)
types['shell_str'] = shell_str

schema = {
    'jpdg_root': 'shell_str',
    'subjects' : {
        conf.UNDEFINED_KEYS: {
            'base_dir': 'shell_str',
            'classpath': 'shell_str',
            'lib_dirs': [ 'shell_str' ],
            'class_dirs': [ 'shell_str' ],
            'exclude_pkgs': [ 'shell_str' ],
            'target': 'shell_str',
            'targets': [ 'shell_str' ],
        },
    }
}

CONFIG_PATHS = list()

HOME = os.environ.get('HOME', None)
if HOME is not None:
    CONFIG_PATHS.append(os.path.join(HOME, '.jpdg-subjects.conf'))

CONFIG_PATHS.append( os.path.join(os.getcwd(), 'subjects.conf'))


class Configuration(conf.BaseConfig):

    def __new__(cls, paths, local_updates=None):
        all_paths = list(CONFIG_PATHS)
        all_paths += paths
        return super(Configuration, cls).__new__(
                cls, schema, *all_paths, types=types, local_updates=local_updates)

    def _export_dict(self):
        d = copy.deepcopy(self._d)
        for name, subject in d['subjects'].iteritems():
            d['subjects'][name] = self.annotate(d, subject)
        self._exposed = self._create_section(d)

    def annotate(self, conf, subject):
        subject = dict(subject)
        lib_jars = list()
        for lib_dir in subject['lib_dirs']:
            lib_jars += glob.glob(os.path.join(lib_dir, '*.jar'))
            lib_jars += glob.glob(os.path.join(lib_dir, '*.aar'))
        subject['lib_jars'] = lib_jars
        subject['soot_classpath'] = ':'.join((
            ':'.join(subject['class_dirs']),
            ':'.join(subject['lib_jars']),
            subject['classpath'],
        ))
        targets = list()
        if 'target' in subject:
            targets.append(subject['target'])
        else:
            targets.extend(subject['targets'])
        subject['jpdg_cmd'] = [
            'java',
            '-Xmx8g',
            '-jar',
            self.jpdg_jar,
            '-c', subject['soot_classpath'],
            '-l', 'op',
        ]
        for t in targets:
            subject['jpdg_cmd'] += ['-d', t]
        for ex_dir in subject['exclude_pkgs']:
            subject['jpdg_cmd'] += ['-e', ex_dir]
        return subject

    @property
    def jpdg_jar(self):
        return os.path.join(self._d['jpdg_root'], 'target', 'jpdg-git-master.jar')

    @property
    def parsemis_jar(self):
        return os.path.join(self._d['jpdg_root'], 'parsemis', 'target', 'parsemis-git-master.jar')

    @property
    def parsemis_cmd(self):
        return [
            'java',
            '-Xmx4g',
            '-jar',
            self.parsemis_jar,
        ]

