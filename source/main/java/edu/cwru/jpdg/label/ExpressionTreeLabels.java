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

public class ExpressionTreeLabels implements LabelMaker {

    public String label(pDG_Builder pDG, int uid, Block b) {
        HashMap<Integer,Node> var_ops = new HashMap<Integer,Node>();
        TreeBuilder tree_builder = new TreeBuilder(var_ops);

        String tail_label = "";

        if (b.getTail() instanceof soot.jimple.IfStmt) {
            soot.jimple.IfStmt branch = (soot.jimple.IfStmt)b.getTail();
            tail_label = tree_builder.build(branch.getCondition()).toString();
        }

        for (Iterator<soot.Unit> it = b.iterator(); it.hasNext(); ) {
            soot.Unit u = it.next();
            if (u instanceof DefinitionStmt) {
                DefinitionStmt def_stmt = (DefinitionStmt)u;
                soot.Local def_var = null;
                String use_str = "";
                try {
                    def_var  = (soot.Local)def_stmt.getLeftOp();
                } catch (java.lang.ClassCastException e) {
                    continue;
                }
                soot.Value right = def_stmt.getRightOp();
                Node node = tree_builder.build(right);

                if (node != null) {
                    var_ops.put(def_var.getNumber(), node);
                }
            } else if (u instanceof soot.jimple.Stmt) {
                soot.jimple.Stmt stmt = (soot.jimple.Stmt)u;
                if (stmt.containsInvokeExpr()) {
                    tail_label += tree_builder.build(stmt.getInvokeExpr());
                }
            }
        }

        String label = "";
        List<soot.Local> live_vars = pDG.ddg_builder.sll.getLiveLocalsAfter(b.getTail());
        for (soot.Local live_var : live_vars) {
            if (var_ops.containsKey(live_var.getNumber())) {
                // label += live_var.toString() + "=";
                label += var_ops.get(live_var.getNumber()).toString();
                label += ";";
            }
        }
        label += tail_label;
        // return "block uid: " + uid + "\n" + b.toString() + "\n" + label + "\n" + live_vars;
        return label;
    }

}

class TreeBuilder {

    Map<Integer,Node> var_ops;

    TreeBuilder(Map<Integer,Node> var_ops) {
        this.var_ops = var_ops;
    }

    Node build(soot.Value value) {
        return process_value(value);
    }

    Node process_value(soot.Value right) {
        if (right instanceof soot.jimple.Ref) {
            return process_ref((soot.jimple.Ref)right);
        } else if (right instanceof soot.jimple.Expr) {
            return process_expr((soot.jimple.Expr)right);
        } else if (right instanceof soot.Local) {
            return process_local((soot.Local)right);
        } else if (right instanceof soot.Immediate) {
            return process_immediate((soot.Immediate)right);
        }
        throw new RuntimeException("Unexpected type " + right.getClass());
    }

    Node process_ref(soot.jimple.Ref ref) {
        return new Node(ref.getType());
    }

    Node process_immediate(soot.Immediate konst) {
        return
          (new Node(konst.getType()))
            .addkid(new Node(konst.toString()));
    }

    Node process_expr(soot.jimple.Expr expr) {
        if (expr instanceof soot.jimple.BinopExpr) {
            return process_binop((soot.jimple.BinopExpr)expr);
        } else if (expr instanceof soot.jimple.AnyNewExpr) {
            return process_any_new_expr((soot.jimple.AnyNewExpr)expr);
        } else if (expr instanceof soot.jimple.InvokeExpr) {
            return process_invoke((soot.jimple.InvokeExpr)expr);
        } else if (expr instanceof soot.jimple.InstanceOfExpr) {
            return process_instance_of((soot.jimple.InstanceOfExpr)expr);
        } else if (expr instanceof soot.jimple.CastExpr) {
            return process_cast((soot.jimple.CastExpr)expr);
        } else if (expr instanceof soot.jimple.UnopExpr) {
            return process_unop((soot.jimple.UnopExpr)expr);
        }
        throw new RuntimeException("Unexpected type " + expr.getClass());
    }

    Node process_local(soot.Local var) {
        if (var_ops.containsKey(var.getNumber())) {
            return var_ops.get(var.getNumber());
        }
        return new Node(var.getType());
    }

    Node process_binop(soot.jimple.BinopExpr expr) {
        return
          (new Node(expr.getSymbol().trim()))
            .addkid(process_value(expr.getOp1()))
            .addkid(process_value(expr.getOp2()));
    }

    Node process_instance_of(soot.jimple.InstanceOfExpr expr) {
        return
          (new Node("instanceof"))
            .addkid(process_value(expr.getOp()))
            .addkid(new Node(expr.getCheckType()));
    }

    Node process_cast(soot.jimple.CastExpr expr) {
        return
          (new Node("cast"))
            .addkid(new Node(expr.getCastType()))
            .addkid(process_value(expr.getOp()));
    }

    Node process_unop(soot.jimple.UnopExpr expr) {
        if (expr instanceof soot.jimple.LengthExpr) {
            return process_length_op((soot.jimple.LengthExpr)expr);
        } else if (expr instanceof soot.jimple.NegExpr) {
            return process_neg_op((soot.jimple.NegExpr)expr);
        }
        throw new RuntimeException("Unexpected type " + expr.getClass());
    }

    Node process_length_op(soot.jimple.LengthExpr expr) {
        return (new Node("length")).addkid(process_value(expr.getOp()));
    }

    Node process_neg_op(soot.jimple.NegExpr expr) {
        return (new Node("negate")).addkid(process_value(expr.getOp()));
    }

    Node process_invoke(soot.jimple.InvokeExpr expr) {
        soot.SootMethod m = expr.getMethod();
        Node params = new Node("params");
        Node returns = new Node("return");
        for (soot.Type type : m.getParameterTypes()) {
            params.addkid(new Node(type));
        }
        Node n =(new Node("call"))
            .addkid(new Node(m.getDeclaringClass().getName() + "." + m.getName()))
            .addkid(params)
            .addkid(returns.addkid(new Node(m.getReturnType())));
        return n;
    }

    Node process_any_new_expr(soot.jimple.AnyNewExpr expr) {
        if (expr instanceof soot.jimple.NewArrayExpr) {
            return process_new_array((soot.jimple.NewArrayExpr)expr);
        } else if (expr instanceof soot.jimple.NewMultiArrayExpr) {
            return process_new_multi_array((soot.jimple.NewMultiArrayExpr)expr);
        } else if (expr instanceof soot.jimple.NewExpr) {
            return process_new_expr((soot.jimple.NewExpr)expr);
        }
        throw new RuntimeException("Unexpected type " + expr.getClass());
    }

    Node process_new_array(soot.jimple.NewArrayExpr expr) {
        return
          (new Node("new-array"))
            .addkid(new Node(expr.getBaseType()))
            .addkid(process_value(expr.getSize()));
    }

    Node process_new_multi_array(soot.jimple.NewMultiArrayExpr expr) {
        Node n =
          (new Node("new-multi-array"))
            .addkid(new Node(expr.getBaseType()));
        for (Object size : expr.getSizes()) {
            n.addkid(process_value((soot.Value)size));
        }
        return n;
    }

    Node process_new_expr(soot.jimple.NewExpr expr) {
        return
          (new Node("new"))
            .addkid(new Node(expr.getBaseType()));
    }
}

class Node {

    Object label;
    List<Node> children = new ArrayList<Node>();

    Node(Object label) {
        this.label = label;
    }

    Node addkid(Node kid) {
        if (kid == null) {
            throw new NullPointerException("kid can't be null");
        }
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
        return kid_count() + ":" + label.toString();
        // return label.toString();
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
