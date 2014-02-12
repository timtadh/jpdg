package test.labels;

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

public class Stmts {

    public void arith_ops() {
        int a = 0;
        int b = 0;
        int c = 0;
        c = a + b;
        c = a - b;
        c = a * b;
        c = a / b;
        c = a % b;
    }

    public void un_ops() {
        int a = 0;
        int[] arr = new int[3];
        a = arr.length;
        a = -a;
    }

    public void bit_ops() {
        int a = 0;
        int b = 0;
        int c = 0;
        c = a & b;
        c = a | b;
        c = a ^ b;
        c = a << b;
        c = a >> b;
        c = a >>> b;
    }

    public void cmp_ops() {
        int a = 0;
        int b = 0;
        boolean c = false;
        c = a == b;
        c = a != b;
        c = a < b;
        c = a <= b;
        c = a > b;
        c = a >= b;
        c = (new Integer(3)) instanceof Object;
    }

    public void new_ops() {
        Object o = new Object();
        int[] a = new int[3];
        int[][] m = new int[5][4];
    }

    public void call_stmt() {
        Object o = new Object();
        o.hashCode();
    }

}
