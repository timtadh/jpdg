package main

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
  "os"
  "fmt"
  "path"
  "io"
  "io/ioutil"
  "compress/gzip"
  "strings"
)

import (
  "github.com/timtadh/getopt"
)

import (
  "cwru/selab/slice/graph"
)

var ErrorCodes map[string]int = map[string]int{
    "usage":1,
    "version":2,
    "opts":3,
    "badint":5,
}

var UsageMessage string = "slice [options] <graphs>"
var ExtendedMessage string = `
slices a graph around a set of nodes with respect to a direction

Options
    -h, --help                          print this message
    -s, --stdin                         read from stdin instead

Specs
    <graphs>                            path to graph files

Graph File Format
    The graph file format is a line delimited format with vertex lines and edge
    lines. For example:

        vertex	{"id":136,"label":""}
        edge	{"src":23,"targ":25,"label":"ddg"}

    Format:

        line -> vertex "\n"
              | edge "\n"

        vertex -> "vertex" "\t" vertex_json

        edge -> "edge" "\t" edge_json

        vertex_json -> {"id": int, "label": string, ...}
        // other items are optional

        edge_json -> {"src": int, "targ": int, "label": int, ...}
        // other items are  optional
`

func Usage(code int) {
    fmt.Fprintln(os.Stderr, UsageMessage)
    if code == 0 {
        fmt.Fprintln(os.Stderr, ExtendedMessage)
        code = ErrorCodes["usage"]
    } else {
        fmt.Fprintln(os.Stderr, "Try -h or --help for help")
    }
    os.Exit(code)
}

func Input(input_path string) (reader io.Reader, closeall func()) {
    stat, err := os.Stat(input_path)
    if err != nil { panic(err) }
    if stat.IsDir() {
        return InputDir(input_path)
    } else {
        return InputFile(input_path)
    }
}

func InputFile(input_path string) (reader io.Reader, closeall func()) {
    freader, err := os.Open(input_path)
    if err != nil { panic(err) }
    if strings.HasSuffix(input_path, ".gz") {
        greader, err := gzip.NewReader(freader)
        if err != nil { panic(err) }
        return greader, func() {
            greader.Close()
            freader.Close()
        }
    }
    return freader, func() {
        freader.Close()
    }
}

func InputDir(input_dir string) (reader io.Reader, closeall func()) {
    var readers []io.Reader
    var closers []func()
    dir, err := ioutil.ReadDir(input_dir)
    if err != nil { panic(err) }
    for _, info := range dir {
        if info.IsDir() { continue }
        creader, closer := InputFile(path.Join(input_dir, info.Name()))
        readers = append(readers, creader)
        closers = append(closers, closer)
    }
    reader = io.MultiReader(readers...)
    return reader, func() {
        for _, closer := range closers {
            closer()
        }
    }
}

func main() {

    args, optargs, err := getopt.GetOpt(
        os.Args[1:],
        "hs",
        []string{
          "help",
          "stdin",
        },
    )
    if err != nil {
        fmt.Fprintln(os.Stderr, err)
        Usage(ErrorCodes["opts"])
    }

    stdin := false
    for _, oa := range optargs {
        switch oa.Opt() {
        case "-h", "--help": Usage(0)
        case "-s", "--stdin": stdin = true
        }
    }

    var reader io.Reader
    var close_reader func()
    if stdin {
        reader = os.Stdin
        close_reader = func() {}
    } else {
        if len(args) != 1 {
            fmt.Fprintln(os.Stderr, "Expected a path to the graph file")
            Usage(ErrorCodes["opts"])
        }
        reader, close_reader = Input(args[0])
    }
    defer close_reader()

    type json_object map[string]interface{}


    G, err := graph.LoadGraph(reader)

    slices := G.Slice("3:call,0:java.lang.StringBuilder.append,1:params,0:java.lang.String,1:return,0:java.lang.StringBuilder;")
    for _, slice := range slices {
        g, err := slice.Serialize()
        if err != nil {
            panic(err)
        }
        fmt.Println(string(g))
    }
}

