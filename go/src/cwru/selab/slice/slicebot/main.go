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
  "encoding/json"
)

import (
  "github.com/timtadh/getopt"
  "github.com/timtadh/netutils"
  "github.com/timtadh/queued/net"
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

func Load(path string) (G *graph.Graph, err error) {
    defer func() {
        if e := recover(); e != nil {
            if E, ok := e.(error); ok {
                err = E
            } else {
                err = fmt.Errorf("%v", e)
            }
        }
    }()
    reader, close_reader := Input(path)
    defer close_reader()
    return graph.LoadGraph(reader)
}

type Candidate struct {
    Label string
    Count int
}

func (self *Candidate) String() string {
    return fmt.Sprintf("%d, %s", self.Count, self.Label)
}

type Candidates []*Candidate

func (self Candidates) String() string {
    s := ""
    for _, c := range self {
        s += c.String() + "\n"
    }
    return s
}

func GetCandidates(G *graph.Graph, prefix string, minimum int) (candidates Candidates) {
    for k, v, n := G.Index.PrefixFind([]byte(prefix))(); n != nil; k, v, n = n() {
        label := string(k.(types.ByteSlice))
        matches := v.([]*graph.Vertex)
        if len(matches) >= minimum {
            candidates = append(candidates, &Candidate{label, len(matches)})
        }
    }
    return candidates
}

func ErrorHandler() (chan<- error) {
    errors := make(chan error)
    go func() {
        for err := range errors {
            fmt.Fprintln(os.Stderr, "ERROR-LOG", err)
        }
    }()
    return errors
}

func CmdParse(line []byte) (cmd string, rest []byte, err error) {
    defer func() {
        if e := recover(); e != nil {
            if E, ok := e.(error); ok {
                err = E
            } else {
                err = fmt.Errorf("%v", e)
            }
        }
    }()
    cmd, rest = net.DecodeMessage(line)
    return cmd, rest, nil
}

type State interface {
    HandleError(err error)
    Command(cmd string, rest []byte) State
    CloseConnection() bool
}

type BaseState struct{
    Writer chan<- []byte
    Errors chan<- error
}

type StartState struct{
    BaseState
}

type LoadedState struct{
    BaseState
    G *graph.Graph
}

func (self *BaseState) CloseConnection() bool {
    return false
}

func (self *BaseState) HandleError(err error) {
    self.Errors<-err
    self.Writer<-net.EncodeMessage("ERROR", []byte(err.Error()))
}

func (self *StartState) Command(cmd string, rest []byte) State {
    switch cmd {
    case "LOAD":
        G, err := Load(string(rest))
        if err != nil {
            self.HandleError(err)
        } else {
            self.Writer<-net.EncodeMessage("OK", nil)
            return &LoadedState{self.BaseState, G}
        }
    default:
        self.HandleError(fmt.Errorf("unexpected command, '%s'", cmd))
    }
    return self
}

func MakeArgv(line string) (argv []string) {
    split := strings.Split(line, " ")
    for _, s := range split {
        s = strings.TrimSpace(s)
        if s != "" {
            argv = append(argv, s)
        }
    }
    return argv
}

func ParseSlice(argv []string) (prefix string, direction graph.Direction, filtered_edges map[string]bool, err error) {
    _, optargs, err := getopt.GetOpt(
        argv,
        "p:d:e:",
        []string{
          "prefix=",
          "direction=",
          "edge-filter=",
        },
    )
    if err != nil {
        return "", nil, nil, err
    }
    prefix = ""
    direction = graph.Backward
    filtered_edges = make(map[string]bool)
    for _, oa := range optargs {
        switch oa.Opt() {
        case "-p", "--prefix": prefix = oa.Arg()
        case "-d", "--direction":
            direction = ParseDirection(oa.Arg())
        case "-e", "--edge-filter":
            filtered_edges[oa.Arg()] = true
        }
    }
    if prefix == "" {
        return "", nil, nil, fmt.Errorf("You must supply a prefix")
    }
    return prefix, direction, filtered_edges, nil
}

func ParseSubGraph(argv []string) (nodes []int64, filtered_edges map[string]bool, err error) {
    args, optargs, err := getopt.GetOpt(
        argv,
        "e:",
        []string{
          "edge-filter=",
        },
    )
    if err != nil {
        return nil, nil, err
    }

    filtered_edges = make(map[string]bool)
    for _, oa := range optargs {
        switch oa.Opt() {
        case "-e", "--edge-filter":
            filtered_edges[oa.Arg()] = true
        }
    }

    for _, s := range args {
        i, err := strconv.Atoi(s)
        if err != nil {
            return nil, nil, err
        } else {
            nodes = append(nodes, int64(i))
        }
    }

    return nodes, filtered_edges, nil
}

func ParsePartition(argv []string) (attr string, filtered_edges map[string]bool, err error) {
    _, optargs, err := getopt.GetOpt(
        argv,
        "e:a:",
        []string{
          "edge-filter=",
          "attr=",
        },
    )
    if err != nil {
        return "", nil, err
    }

    filtered_edges = make(map[string]bool)
    for _, oa := range optargs {
        switch oa.Opt() {
        case "-e", "--edge-filter":
            filtered_edges[oa.Arg()] = true
        case "-a", "--attr":
            attr = oa.Arg()
        }
    }

    return attr, filtered_edges, nil
}

func SerializeGraphs(graphs []*graph.Graph) ([]byte, error) {
    var s []byte
    for _, g := range graphs {
        bytes, err := g.Serialize()
        if err != nil {
            return nil, err
        }
        s = append(s, bytes...)
        s = append(s, []byte("\n")...)
    }
    s = append(s, []byte("\n")...)
    return s, nil
}

func (self *LoadedState) Command(cmd string, rest []byte) State {
    switch cmd {
    case "CANDIDATES":
        self.Writer<-net.EncodeMessage(
            "CANDIDATES",
            []byte(
                GetCandidates(self.G, string(rest), 1).String(),
            ),
        )
    case "SLICE":
        prefix, dir, fe, err := ParseSlice(MakeArgv(string(rest)))
        if err != nil {
            self.HandleError(err)
        } else {
            slices := self.G.Slice(prefix, dir, fe)
            s, err := SerializeGraphs(slices)
            if err != nil {
                self.HandleError(err)
            } else {
                self.Writer<-net.EncodeMessage("GRAPHS", s)
            }
        }
    case "NODE":
        i, err := strconv.Atoi(string(rest))
        if err != nil {
            self.HandleError(err)
        } else {
            node := self.G.V[int64(i)]
            bytes, err := json.Marshal(node.Rest)
            if err != nil {
                self.HandleError(err)
            } else {
                self.Writer<-net.EncodeMessage("NODE", bytes)
            }
        }
    case "SUBGRAPH":
        nodes, filtered_edges, err := ParseSubGraph(MakeArgv(string(rest)))
        if err != nil {
            self.HandleError(err)
        } else {
            g, err := self.G.SubGraph(nodes, filtered_edges).Serialize()
            if err != nil {
                self.HandleError(err)
            } else {
                self.Writer<-net.EncodeMessage("GRAPH", g)
            }
        }
    case "PARTITION":
        attr, filtered_edges, err := ParsePartition(MakeArgv(string(rest)))
        if err != nil {
            self.HandleError(err)
        } else {
            partition, err := self.G.Partition(attr, filtered_edges)
            if err != nil {
                self.HandleError(err)
            } else {
                s, err := SerializeGraphs(partition)
                if err != nil {
                    self.HandleError(err)
                } else {
                    self.Writer<-net.EncodeMessage("GRAPHS", s)
                }
            }
        }
    default:
        self.HandleError(fmt.Errorf("unexpected command, '%s'", cmd))
    }
    return self
}

func Connection(reader <-chan byte, writer chan<- []byte, errors chan<- error) {
    var state State = &StartState{BaseState{writer, errors}}
    lines := netutils.Readlines(reader)
    for line := range lines {
        cmd, rest, err := CmdParse(line)
        if err != nil {
            state.HandleError(err)
        } else {
            state = state.Command(cmd, rest)
        }
        if state.CloseConnection() {
            break
        }
    }
    close(writer)
}

func main() {

    _, optargs, err := getopt.GetOpt(
        os.Args[1:],
        "h",
        []string{
          "help",
        },
    )
    if err != nil {
        fmt.Fprintln(os.Stderr, err)
        Usage(ErrorCodes["opts"])
    }

    for _, oa := range optargs {
        switch oa.Opt() {
        case "-h", "--help": Usage(0)
        }
    }

    errors := ErrorHandler()
    Connection(netutils.Reader(os.Stdin, errors), netutils.Writer(os.Stdout, errors), errors)
}


