package test.pDG;

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
import java.nio.*;

public class Buffer {

    public static void main(String[] args) {
        Buffer self = new Buffer(250);
    }

    ByteBuffer buf;

    Buffer(int cap) {
        buf = ByteBuffer.allocate(cap);
    }

    public byte[] read(int pos, int length) {
        if (pos > buf.limit()) {
            throw new java.lang.RuntimeException("pos greater than limit");
        } else if (pos + length > buf.limit()) {
            throw new java.lang.RuntimeException("pos + length greater than limit");
        }
        byte[] into = new byte[length];
        buf.position(pos);
        buf.get(into);
        buf.rewind();
        return into;
    }

    public void put(int pos, byte[] from) {
        if (pos > buf.limit()) {
            throw new java.lang.RuntimeException("pos greater than limit");
        } else if (pos + from.length > buf.limit()) {
            throw new java.lang.RuntimeException("pos + length greater than limit");
        }
        buf.position(pos);
        buf.put(from);
        buf.rewind();
    }

    public void putAfter(byte[] leader, byte[] from) {
        if (leader.length > buf.limit()) {
            throw new java.lang.RuntimeException("leader is longer than the buffer");
        }
        buf.position(0);
        for (; buf.position() < buf.limit();) {
            // TODO: finish this method;
            break;
        }
    }
}
