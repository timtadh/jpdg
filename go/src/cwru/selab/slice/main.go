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
  "encoding/json"
  "io"
  "io/ioutil"
  "compress/gzip"
  "strings"
  "github.com/timtadh/getopt"
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

func ProcessLines(reader io.Reader, process func(string)) {

    const SIZE = 4096

    read_chunk := func() (chunk []byte, closed bool) {
        chunk = make([]byte, 4096)
        if n, err := reader.Read(chunk); err == io.EOF {
            return nil, true
        } else if err != nil {
            panic(err)
        } else {
            return chunk[:n], false
        }
    }

    parse := func(buf []byte) (obuf []byte, line string, ok bool) {
        for i := 0; i < len(buf); i++ {
            if buf[i] == '\n' {
                line = string(buf[:i+1])
                obuf = buf[i+1:]
                return obuf, line, true
            }
        }
        return buf, "", false
    }

    var buf []byte
    read_line := func() (line string, closed bool) {
        ok := false
        buf, line, ok = parse(buf)
        for !ok {
            chunk, closed := read_chunk()
            if closed || len(chunk) == 0 {
                line = string(buf)
                return line, true
            }
            buf = append(buf, chunk...)
            buf, line, ok = parse(buf)
        }
        return line, false
    }

    closed := false
    for !closed {
        var line string
        line, closed = read_line()
        process(line)
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

    parse_line := func(line string) (line_type string, data []byte) {
        split := strings.Split(line, "\t")
        return strings.TrimSpace(split[0]), []byte(strings.TrimSpace(split[1]))
    }
    load_vertex := func(obj map[string]interface{}) (id int, label string) {
        return int(obj["id"].(float64)), obj["label"].(string)
    }
    load_edge := func(obj map[string]interface{}) (src, targ int, label string) {
        return int(obj["src"].(float64)), int(obj["targ"].(float64)), obj["label"].(string)
    }

    o_vertices := make(map[int]string)
    o_edges := make(map[int]string)
    vertices := make(map[int]string)
    edges := make(map[int][]int)
    redges := make(map[int][]int)
    ProcessLines(reader, func(line string) {
        if len(line) == 0 || !strings.Contains(line, "\t") {
            return
        }
        line_type, data := parse_line(line)

        var obj map[string]interface{}
        err = json.Unmarshal(data, &obj)
        if err != nil {
            fmt.Fprintln(os.Stderr, "Could not load json", data, err)
            return
        }
        switch line_type {
        case "vertex":
            id, label := load_vertex(obj)
            vertices[id] = label
        case "edge":
            src, targ, _ := load_edge(obj)
            edges[src] = append(edges[src], targ)
            redges[targ] = append(redges[targ], src)
        default:
            fmt.Fprintln(os.Stderr, "Unknown line type", line_type)
        }
    })

    fmt.Println(vertices)
    fmt.Println(edges)
    fmt.Println(redges)
}

