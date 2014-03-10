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
  "strconv"
)

import (
  "github.com/timtadh/getopt"
  "github.com/timtadh/data-structures/types"
)

import (
  "cwru/selab/slice/graph"
)

var ErrorCodes map[string]int = map[string]int{
    "usage":1,
    "version":2,
    "opts":3,
    "badint":5,
    "baddir":6,
}

var UsageMessage string = "slice [options] --prefix=<string> <graphs>"
var ExtendedMessage string = `
slices a graph around a set of nodes with respect to a direction

Options
    -h, --help                          print this message
    -s, --stdin                         read from stdin instead
    -p, --prefix=<string>               select nodes from this prefix
    -d, --direction=<directino>         slice direction (default backward)
    -c, --candidates=<int> > 0          show labels with counts greater than
                                        <int>
    -e, --edge-filter=<string>          filter edges of type <string> (can be
                                        specified multiple times)

Specs
    <graphs>                            path to graph files
    <direction>                         {backward, forward, both}

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
        fmt.Fprintln(os.Stdout, ExtendedMessage)
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

func ParseInt(str string) int {
    i, err := strconv.Atoi(str)
    if err != nil {
        fmt.Fprintf(os.Stderr, "Error parsing '%v' expected an int\n", str)
        Usage(ErrorCodes["badint"])
    }
    return i
}

func ParseDirection(str string) graph.Direction {
    switch str {
    case "forward":
        return graph.Forward
    case "backward":
        return graph.Backward
    case "both":
        return graph.Both
    }
    fmt.Fprintf(os.Stderr, "Error parsing '%v' expect one of {forward, backward, both}\n", str)
    Usage(ErrorCodes["baddir"])
    panic("unreachable")
}

func main() {

    args, optargs, err := getopt.GetOpt(
        os.Args[1:],
        "hsp:c:d:e:",
        []string{
          "help",
          "stdin",
          "prefix=",
          "candidates=",
          "direction=",
          "edge-filter=",
        },
    )
    if err != nil {
        fmt.Fprintln(os.Stderr, err)
        Usage(ErrorCodes["opts"])
    }

    stdin := false
    candidates := false
    minimum := 0
    prefix := ""
    direction := graph.Backward
    filtered_edges := make(map[string]bool)
    for _, oa := range optargs {
        switch oa.Opt() {
        case "-h", "--help": Usage(0)
        case "-s", "--stdin": stdin = true
        case "-p", "--prefix": prefix = oa.Arg()
        case "-c", "--candidates":
            candidates = true
            minimum = ParseInt(oa.Arg())
        case "-d", "--direction":
            direction = ParseDirection(oa.Arg())
        case "-e", "--edge-filter":
            filtered_edges[oa.Arg()] = true
        }
    }
    if prefix == "" && !candidates {
        fmt.Fprintln(os.Stderr, "You must specify a prefix to slice on")
        Usage(ErrorCodes["opts"])
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

    if candidates {
        for k, v, n := G.Index.PrefixFind([]byte(prefix))(); n != nil; k, v, n = n() {
            label := string(k.(types.ByteSlice))
            count := len(v.([]*graph.Vertex))
            if count >= minimum {
                fmt.Println(label, count)
            }
        }
        return
    }

    slices := G.Slice(prefix, direction, filtered_edges)
    for _, slice := range slices {
        g, err := slice.Serialize()
        if err != nil {
            panic(err)
        }
        fmt.Println(string(g))
    }
}

