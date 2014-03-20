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
import soot.toolkits.graph.BriefUnitGraph;


public class UnitBlockGraph extends BlockGraph {

    public  UnitBlockGraph(Body body) {
        this(new BriefUnitGraph(body));
    }

    public  UnitBlockGraph(UnitGraph unitGraph) {
        super(unitGraph);
        soot.util.PhaseDumper.v().dumpGraph(this, mBody);
    }

    @Override
    protected Set<Unit> computeLeaders(UnitGraph unitGraph) {
        Set<Unit> leaders = new HashSet<Unit>();
        Body body = unitGraph.getBody();
        for (Iterator<Unit> it = body.getUnits().iterator(); it.hasNext(); ) {
            Unit u = it.next();
            leaders.add(u);
        }
        return leaders;
    }
}
