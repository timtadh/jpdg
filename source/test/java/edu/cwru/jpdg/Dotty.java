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

import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.*;

import java.lang.Runtime;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.lang.Thread;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class Dotty {

    static final String cwd = System.getProperty("user.dir");

    /**
     * Compiles the graph to dotty.
     */
    public static String dotty(String graph) {

        byte[] bytes = graph.getBytes();

        try {
            ProcessBuilder pb = new ProcessBuilder("dotty");
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            OutputStream stdin = p.getOutputStream();
            stdin.write(bytes);
            stdin.close();

            String line;
            BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            List<String> stdout_lines = new ArrayList<String>();
            line = stdout.readLine();
            while (line != null) {
                stdout_lines.add(line);
                line = stdout.readLine();
            }
            if (p.waitFor() != 0) {
                throw new RuntimeException("javac failed");
            }

            return StringUtils.join(stdout_lines, "\n");
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void graphviz(String name, String graph) {

        byte[] bytes = graph.getBytes();

        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", "-o", "reports/" + name + ".png");
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            OutputStream stdin = p.getOutputStream();
            stdin.write(bytes);
            stdin.close();

            String line;
            BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            List<String> stdout_lines = new ArrayList<String>();
            line = stdout.readLine();
            while (line != null) {
                stdout_lines.add(line);
                line = stdout.readLine();
            }
            if (p.waitFor() != 0) {
                throw new RuntimeException("javac failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}

