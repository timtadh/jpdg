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

import java.util.*;

import org.apache.commons.lang.StringUtils;

import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefUnitGraph;

import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

import soot.jimple.DefinitionStmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.internal.JimpleLocalBox;

import edu.cwru.jpdg.pDG_Builder;

public class SimpleLabels implements LabelMaker {

    public String label(pDG_Builder pDG, int uid, Block b) {
        System.out.println("block uid: " + uid);

        HashMap<Integer,Node> var_ops = new HashMap<Integer,Node>();

        String tail_label = "";

        if (b.getTail() instanceof soot.jimple.IfStmt) {
            tail_label = "(branch)";
        }

        for (Iterator<soot.Unit> it = b.iterator(); it.hasNext(); ) {
            soot.Unit u = it.next();
            System.out.println(u);
            System.out.println(u.getClass());
            if (u instanceof DefinitionStmt) {
                DefinitionStmt def_stmt = (DefinitionStmt)u;
                System.out.println(def_stmt.getRightOpBox().getClass());
                System.out.println(def_stmt.getRightOpBox().getValue().getClass());
                soot.Local def_var = null;
                String use_str = "";
                try {
                    def_var  = (soot.Local)def_stmt.getLeftOp();
                } catch (java.lang.ClassCastException e) {
                    continue;
                }
                soot.Value right = def_stmt.getRightOp();
                Node node = process_value(var_ops, right);


                if (node != null) {
                    var_ops.put(def_var.getNumber(), node);
                }
            }
            System.out.println();
        }

        for (Map.Entry<Integer,Node> entry : var_ops.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
            System.out.println();
        }

        String label = "";
        List<soot.Local> live_vars = pDG.ddg_builder.sll.getLiveLocalsAfter(b.getTail());
        for (soot.Local live_var : live_vars) {
            System.out.println("live>>> "  + live_var);
            if (var_ops.containsKey(live_var.getNumber())) {
                label += live_var.toString() + ":";
                label += var_ops.get(live_var.getNumber()).toString();
                label += ";";
            }
        }
        label += tail_label;
        System.out.println();
        System.out.println();
        System.out.println();
        return "block uid: " + uid + "\n" + b.toString() + "\n" + label + "\n" + live_vars;
    }

    Node process_value(Map<Integer,Node> var_ops, soot.Value right) {
        if (right instanceof JimpleLocal) {
            return process_local(var_ops, (JimpleLocal)right);
        } else if (right instanceof soot.jimple.Constant) {
            return new Node(right.getType());
        } else if (right instanceof soot.jimple.Ref) {
            return new Node(right.getType());
        } else if (right instanceof soot.jimple.Expr) {
            System.out.println(">> soot.jimple.Expr >> " + right);
            return process_expr(var_ops, (soot.jimple.Expr)right);
        } else {
            System.out.println(">>?>> " + right.getClass());
        }
        return null;
    }

    Node process_local(Map<Integer,Node> var_ops, JimpleLocal var) {
        if (var_ops.containsKey(var.getNumber())) {
            return var_ops.get(var.getNumber());
        }
        return new Node(var.getType());
    }

    Node process_expr(Map<Integer,Node> var_ops, soot.jimple.Expr expr) {
        if (expr instanceof soot.jimple.BinopExpr) {
            return process_binop(var_ops, (soot.jimple.BinopExpr)expr);
        } else {
            System.out.println(">>?>> " + expr.getClass());
        }
        return null;
    }

    Node process_binop(Map<Integer,Node> var_ops, soot.jimple.BinopExpr expr) {
        return (new Node(expr.getSymbol().trim())).addkid(
            process_value(var_ops, expr.getOp1())).addkid(
            process_value(var_ops, expr.getOp2()));
    }
}

class Node {

    Object label;
    List<Node> children = new ArrayList<Node>();

    Node(Object label) {
        this.label = label;
    }

    Node addkid(Node kid) {
        children.add(kid);
        return this;
    }

    Node getkid(int i) {
        return children.get(i);
    }

    int kid_count() {
        return children.size();
    }

    String repr() {
        // return kid_count() + ":" + label.toString();
        return label.toString();
    }

    public String toString() {
        List<String> lines = new ArrayList<String>();
        lines.add(this.repr());
        for (Node n : children) {
            lines.add(n.toString());
        }
        return StringUtils.join(lines, ",");
    }

}
