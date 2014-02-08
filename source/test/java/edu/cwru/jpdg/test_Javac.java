package edu.cwru.jpdg;

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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;

import edu.cwru.jpdg.Javac;


public class test_Javac {

    @Test
    public void test_load()
    throws java.io.IOException, java.security.NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(Javac.load("test.parse.HelloWorld").getBytes());
        String digest = new String(Hex.encodeHex(md.digest()));
        assertEquals("796b9a7ee3ea71ba01f85338e01bd8ee5df4d612", digest);
    }

    @Test
    public void test_classes()
    throws java.io.IOException,
      java.security.NoSuchAlgorithmException {
        Map<String, soot.SootClass> classes =
            Javac.classes("test.parse.HelloWorld");
        assertThat(classes.size(), is(2));
        assertThat(
            classes.get("test.parse.HelloWorld").getName(),
            is("test.parse.HelloWorld")
        );
        assertThat(
            classes.get("test.parse.ExtraHelloWorld").getName(),
            is("test.parse.ExtraHelloWorld")
        );
    }
}
