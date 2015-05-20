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

public class OpLabels implements LabelMaker {

    public String label(pDG_Builder pDG, int uid, Block b) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<soot.Unit> it = b.iterator(); it.hasNext(); ) {
            soot.Unit u = it.next();
            sb.append(label(u));
            if (it.hasNext()) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    public String nodeType(Block b) {
        return ExpressionTreeLabels.NodeType(b);
    }

    public String label(soot.Unit u) {
        if (u instanceof AbstractDefinitionStmt) {
            AbstractDefinitionStmt s = (AbstractDefinitionStmt)u;
            return label(s.getLeftOp(), s.getRightOp());
        } else if (u instanceof JIfStmt) {
            JIfStmt s = (JIfStmt)u;
            return label(s.getCondition());
        } else if (u instanceof JGotoStmt) {
            JGotoStmt s = (JGotoStmt)u;
            return "goto";
        } else if (u instanceof JReturnStmt) {
            JReturnStmt s = (JReturnStmt)u;
            return "return";
        }
        throw new RuntimeException(String.format("Unexpected unit %s [%s]", u, u.getClass()));
    }

    public String label(soot.Value left, soot.Value right) {
        String left_label = "";
        if (!(left instanceof JimpleLocal)) {
            throw new RuntimeException(String.format("Unexpected left %s [%s]", left, left.getClass()));
        }
        if ((left instanceof JimpleLocal) && (right instanceof JimpleLocal)) {
            return "=";
        }
        String right_label = label(right);
        return String.format("%s%s", left_label, right_label);
    }

    public String label(soot.Value value) {
        if (value instanceof soot.jimple.ThisRef) {
            return String.format("this");
        } else if (value instanceof soot.jimple.ParameterRef) {
            soot.jimple.ParameterRef pr = (soot.jimple.ParameterRef)value;
            return String.format("param(%d)", pr.getIndex());
        } else if (value instanceof JInstanceFieldRef) {
            JInstanceFieldRef fr = (JInstanceFieldRef)value;
            soot.SootField f = fr.getField();
            return String.format("%s.%s", f.getDeclaringClass(), f.getName());
        } else if (value instanceof soot.jimple.Constant) {
            soot.jimple.Constant c = (soot.jimple.Constant)value;
            return String.format("%s", c);
        } else if (value instanceof soot.jimple.BinopExpr) {
            soot.jimple.BinopExpr binop = (soot.jimple.BinopExpr)value;
            return String.format("%s", binop.getSymbol().trim());
        }
        throw new RuntimeException(String.format("Unexpected value %s [%s]", value, value.getClass()));
    }
}

