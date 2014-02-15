package soot.toolkits.graph;

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

import soot.Body;
import soot.Unit;

import soot.toolkits.graph.pdg.EnhancedUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;


public class ExpandedBlockGraph extends BlockGraph {

    public  ExpandedBlockGraph(Body body) {
        this(new ExceptionalUnitGraph(body));
    }

    public  ExpandedBlockGraph(UnitGraph unitGraph) {
        super(unitGraph);
        soot.util.PhaseDumper.v().dumpGraph(this, mBody);
    }

    @Override
    protected Set<Unit> computeLeaders(UnitGraph unitGraph) {
        Set<Unit> leaders = super.computeLeaders(unitGraph);
        Body body = unitGraph.getBody();
        boolean prev_invoked = false;
        for (Iterator<Unit> it = body.getUnits().iterator(); it.hasNext(); ) {
            Unit u = it.next();
            if (prev_invoked) {
                leaders.add(u);
            }
            prev_invoked = false;
            if (invokes(u)) {
                leaders.add(u);
                prev_invoked = true;
            }
        }
        return leaders;
    }

    boolean invokes(Unit u) {
        if (u instanceof soot.jimple.Stmt) {
            return invokes((soot.jimple.Stmt)u);
        }
        return false;
    }

    boolean invokes(soot.jimple.Stmt stmt) {
        return stmt.containsInvokeExpr();
    }
}

