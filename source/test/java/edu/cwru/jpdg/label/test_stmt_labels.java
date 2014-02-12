package edu.cwru.jpdg.label;

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

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

import soot.jimple.DefinitionStmt;

import edu.cwru.jpdg.pDG_Builder;
import edu.cwru.jpdg.graph.Graph;
import edu.cwru.jpdg.label.*;

import edu.cwru.jpdg.Javac;

public class test_stmt_labels {

    static soot.SootClass klass;
    static HashMap<Integer,Node> var_ops;
    static TreeBuilder builder;

    @BeforeClass
    public static void init() {
        klass = Javac.classes("test.labels.Stmts").get("test.labels.Stmts");
        var_ops = new HashMap<Integer,Node>();
        builder = new edu.cwru.jpdg.label.TreeBuilder(var_ops);
    }

    public List<soot.Unit> method(String name) {
        // System.out.println("method " + name);
        soot.Body body = klass.getMethodByName(name).retrieveActiveBody();
        List<soot.Unit> list = new ArrayList<soot.Unit>();
        int i = 0;
        for (soot.Unit u : body.getUnits()) {
            // System.out.println(i + "   " + u);
            list.add(u);
            i++;
        }
        // System.out.println();
        return list;
    }

    public soot.Value expr(List<soot.Unit> insts, int idx) {
        return ((DefinitionStmt)insts.get(idx)).getRightOp();
    }

    public soot.Value branch(List<soot.Unit> insts, int idx) {
        return ((soot.jimple.IfStmt)insts.get(idx)).getCondition();
    }

    public soot.Value call(List<soot.Unit> insts, int idx) {
        return ((soot.jimple.InvokeStmt)insts.get(idx)).getInvokeExpr();
    }

    @Test
    public void arith_ops() {
        List<soot.Unit> insts = method("arith_ops");
        assertThat(
            builder.build(expr(insts, 4)).toString(),
            is("2:+,0:byte,0:byte")
        );
        assertThat(
            builder.build(expr(insts, 5)).toString(),
            is("2:-,0:byte,0:byte")
        );
        assertThat(
            builder.build(expr(insts, 6)).toString(),
            is("2:*,0:byte,0:byte")
        );
        assertThat(
            builder.build(expr(insts, 7)).toString(),
            is("2:/,0:byte,0:byte")
        );
        assertThat(
            builder.build(expr(insts, 8)).toString(),
            is("2:%,0:byte,0:byte")
        );
    }

    @Test
    public void unary_ops() {
        List<soot.Unit> insts = method("un_ops");
        assertThat(
            builder.build(expr(insts, 3)).toString(),
            is("1:length,0:int[]")
        );
        assertThat(
            builder.build(expr(insts, 4)).toString(),
            is("1:negate,0:int")
        );
    }

    @Test
    public void bit_ops() {
        List<soot.Unit> insts = method("bit_ops");
        assertThat(
            builder.build(expr(insts, 4)).toString(),
            is("2:&,0:byte,0:byte")
        );
        assertThat(
            builder.build(expr(insts, 5)).toString(),
            is("2:|,0:byte,0:byte")
        );
        assertThat(
            builder.build(expr(insts, 6)).toString(),
            is("2:^,0:byte,0:byte")
        );
        assertThat(
            builder.build(expr(insts, 7)).toString(),
            is("2:<<,0:byte,0:byte")
        );
        assertThat(
            builder.build(expr(insts, 8)).toString(),
            is("2:>>,0:byte,0:byte")
        );
        assertThat(
            builder.build(expr(insts, 9)).toString(),
            is("2:>>>,0:byte,0:byte")
        );
    }

    @Test
    public void cmp_ops() {
        List<soot.Unit> insts = method("cmp_ops");
        assertThat(
            builder.build(branch(insts, 9)).toString(),
            is("2:==,0:byte,0:byte")
        );
        assertThat(
            builder.build(branch(insts, 4)).toString(),
            is("2:!=,0:byte,0:byte")
        );
        assertThat(
            builder.build(branch(insts, 29)).toString(),
            is("2:<,0:byte,0:byte")
        );
        assertThat(
            builder.build(branch(insts, 24)).toString(),
            is("2:<=,0:byte,0:byte")
        );
        assertThat(
            builder.build(branch(insts, 19)).toString(),
            is("2:>,0:byte,0:byte")
        );
        assertThat(
            builder.build(branch(insts, 14)).toString(),
            is("2:>=,0:byte,0:byte")
        );
        assertThat(
            builder.build(expr(insts, 36)).toString(),
            is("2:instanceof,0:java.lang.Integer,0:java.lang.Object")
        );
    }

    @Test
    public void new_ops() {
        List<soot.Unit> insts = method("new_ops");
        assertThat(
            builder.build(expr(insts, 1)).toString(),
            is("1:new,0:java.lang.Object")
        );
        assertThat(
            builder.build(expr(insts, 4)).toString(),
            is("2:new-array,0:int,1:int,0:3")
        );
        assertThat(
            builder.build(expr(insts, 5)).toString(),
            is("3:new-multi-array,0:int[][],1:int,0:5,1:int,0:4")
        );
    }

    @Test
    public void call_stmt() {
        List<soot.Unit> insts = method("call_stmt");
        assertThat(
            builder.build(call(insts, 4)).toString(),
            is("3:call,0:java.lang.Object.hashCode,0:params,1:return,0:int")
        );
    }
}

