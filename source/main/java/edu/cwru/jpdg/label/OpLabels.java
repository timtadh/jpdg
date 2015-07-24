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

import java.util.Iterator;

import soot.toolkits.graph.Block;
import soot.jimple.internal.*;

import edu.cwru.jpdg.pDG_Builder;
import edu.cwru.jpdg.graph.Node;

public class OpLabels implements LabelMaker {

    public void postLabel(pDG_Builder pDG, int uid, Block b) {
        for (Iterator<soot.Unit> it = b.iterator(); it.hasNext(); ) {
            soot.Unit u = it.next();
            add_constants(pDG, uid, u);
        }
    }

    public String label(pDG_Builder pDG, Block b) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<soot.Unit> it = b.iterator(); it.hasNext(); ) {
            soot.Unit u = it.next();
            sb.append(label(pDG, u));
            if (it.hasNext()) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    public String nodeType(Block b) {
        return ExpressionTreeLabels.NodeType(b);
    }

    public String label(pDG_Builder pDG, soot.Unit u) {
        if (u instanceof AbstractDefinitionStmt) {
            AbstractDefinitionStmt s = (AbstractDefinitionStmt)u;
            return label(pDG, s.getLeftOp(), s.getRightOp());
        } else if (u instanceof JIfStmt) {
            JIfStmt s = (JIfStmt)u;
            return label(pDG, s.getCondition());
        } else if (u instanceof JGotoStmt) {
            return "goto";
        } else if (u instanceof JReturnStmt) {
            return "return";
        } else if (u instanceof JBreakpointStmt) {
            return "break";
        } else if (u instanceof JEnterMonitorStmt) {
            return "enter monitor";
        } else if (u instanceof JExitMonitorStmt) {
            return "exit monitor";
        } else if (u instanceof JThrowStmt) {
            return "throw";
        } else if (u instanceof JInvokeStmt) {
            JInvokeStmt s = (JInvokeStmt)u;
            return label(pDG, s.getInvokeExpr());
        } else if (u instanceof JLookupSwitchStmt) {
            return "switch";
        } else if (u instanceof JNopStmt) {
            return "nop";
        } else if (u instanceof JRetStmt) {
            return "ret";
        } else if (u instanceof JReturnVoidStmt) {
            return "return";
        } else if (u instanceof JTableSwitchStmt) {
            return "switch";
        }
        throw new RuntimeException(String.format("Unexpected unit %s [%s]", u, u.getClass()));
    }

    public String label(pDG_Builder pDG, soot.Value left, soot.Value right) {
        if ((left instanceof JimpleLocal) && (right instanceof JimpleLocal)) {
            return "=";
        }

        String left_label = null;
        if (left instanceof JimpleLocal) {
            left_label = "";
        } else {
            left_label = label(pDG, left);
        }
        String right_label = null;
        if (right instanceof JimpleLocal) {
            right_label = "";
        } else {
            right_label = label(pDG, right);
        }
        return String.format("%s%s", left_label, right_label);
    }

    public String label(pDG_Builder pDG, soot.Value value) {
        if (value instanceof JimpleLocal) {
            return value.toString();
        } else if (value instanceof soot.jimple.ThisRef) {
            return String.format("this");
        } else if (value instanceof soot.jimple.ParameterRef) {
            soot.jimple.ParameterRef pr = (soot.jimple.ParameterRef)value;
            return String.format("param %d", pr.getIndex());
        } else if (value instanceof JInstanceFieldRef) {
            JInstanceFieldRef fr = (JInstanceFieldRef)value;
            soot.SootField f = fr.getField();
            return String.format("%s.%s", f.getDeclaringClass(), f.getName());
        } else if (value instanceof soot.jimple.StaticFieldRef) {
            soot.jimple.StaticFieldRef fr = (soot.jimple.StaticFieldRef)value;
            soot.SootField f = fr.getField();
            return String.format("%s.%s", f.getDeclaringClass(), f.getName());
        } else if (value instanceof soot.jimple.Constant) {
            soot.jimple.Constant c = (soot.jimple.Constant)value;
            return String.format("const '%s'", c);
        } else if (value instanceof soot.jimple.BinopExpr) {
            soot.jimple.BinopExpr binop = (soot.jimple.BinopExpr)value;
            return String.format("%s", binop.getSymbol().trim());
        } else if (value instanceof JCaughtExceptionRef) {
            JCaughtExceptionRef r = (JCaughtExceptionRef)value;
            return String.format("catch %s", r.getType());
        } else if (value instanceof soot.jimple.InvokeExpr) {
            soot.jimple.InvokeExpr e = (soot.jimple.InvokeExpr)value;
            soot.SootMethod m = e.getMethod();
            return String.format("call %s.%s", m.getDeclaringClass().getName(), m.getName());
        } else if (value instanceof JNewExpr) {
            return "new";
        } else if (value instanceof JNewArrayExpr) {
            return "new-array";
        } else if (value instanceof JNewMultiArrayExpr) {
            return "new-multi-array";
        } else if (value instanceof JCastExpr) {
            return "cast";
        } else if (value instanceof JArrayRef) {
            JArrayRef ar = (JArrayRef)value;
            soot.Value index = ar.getIndex();
            return "array index";
        } else if (value instanceof JInstanceOfExpr) {
            return "instanceof";
        } else if (value instanceof JLengthExpr) {
            return "lengthof";
        } else if (value instanceof JNegExpr) {
            return "negate";
        }
        throw new RuntimeException(String.format("Unexpected value %s [%s]", value, value.getClass()));
    }

    public void add_constants(pDG_Builder pDG, int uid, soot.Unit u) {
        for (soot.ValueBox vb : u.getUseBoxes()) {
            soot.Value v = vb.getValue();
            if (v instanceof soot.jimple.Constant) {
                if (u instanceof AbstractDefinitionStmt) {
                    AbstractDefinitionStmt s = (AbstractDefinitionStmt)u;
                    soot.Value right = s.getRightOp();
                    if (right.equivTo(v)) {
                        continue;
                    }
                }
                add_constant(pDG, uid, u, (soot.jimple.Constant)v);
            }
        }
    }

    public void add_constant(pDG_Builder pDG, int container_uid, soot.Unit container, soot.jimple.Constant c) {
        Node n = pDG.g.nodes.get(container_uid);
        int uid = pDG.g.addNode(
            label(pDG, c),
            c.toString(),
            n.package_name, n.class_name, n.source_file, n.method_name,
            c.getType().toString(),
            n.start_line,
            n.start_column,
            n.end_line,
            n.end_column
        );
        int param = -1;
        try {
            param = pDG.get_param_number(container, (soot.Value)c);
        } catch (pDG_Builder.Error e) {
            throw new RuntimeException(e.toString());
        }
        pDG.g.addEdge(uid, container_uid, String.format("%s:%s", c.getType(), param));
        pDG.g.addEdge(pDG.entry_uid, uid, "");
    }
}

