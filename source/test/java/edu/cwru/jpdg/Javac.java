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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class Javac {

    static final String cwd = System.getProperty("user.dir");
    static final String pathsep =
        System.getProperty("file.separator");

    /**
     * loads a java file from the resources directory. Give the fully
     * qualified name of the java file. eg. for:
     *
     *     source/test/resources/java/test/parse/HelloWorld.java
     *
     * give:
     *
     *     test.parse.HelloWorld
     */
    public static String load(String full_name) {
        ClassLoader loader = Javac.class.getClassLoader();
        String[] split = StringUtils.split(full_name, ".");
        String name = split[split.length-1];
        String slash_name = StringUtils.join(split, pathsep);
        String resource =
            Paths.get("java").resolve(slash_name + ".java").toString();
        InputStream ci = loader.getResourceAsStream(resource);
        BufferedReader bi = new BufferedReader(new InputStreamReader(ci));

        List<String> lines = new ArrayList<String>();
        String line;
        try {
            while ((line = bi.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        lines.add(line);
        return StringUtils.join(lines, "\n");
    }

    static void create_dir(Path dir) {
        try {
            Files.createDirectory(dir);
        } catch (IOException e) {
            System.err.println("Couldn't create directory. Failing");
            throw new RuntimeException("Couldn't create dir");
        }
    }

    /**
     * Compiles the java.
     */
    public static List<String> javac(String basepath, String name, String s) {
        Path dir = Paths.get(cwd).resolve(basepath);
        if (Files.notExists(dir)) { create_dir(dir); }
        Path java = dir.resolve(name + ".java");
        Path build = dir.resolve("build");
        if (!Files.notExists(build)) {
            try {
                FileUtils.deleteDirectory(build.toFile());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't rm -r build dir");
            }
        }
        create_dir(build);

        byte[] bytes = s.getBytes();
        try {
            Files.write(java, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        try {
            Process p = Runtime.getRuntime().exec(
                new String[]{
                    "javac", "-d", build.toString(), java.toString()
                }
            );
            String line;
            BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            List<String> stdout_lines = new ArrayList<String>();
            line = stdout.readLine();
            while (line != null) {
                stdout_lines.add(line);
                line = stdout.readLine();
            }
            BufferedReader stderr = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()));
            List<String> stderr_lines = new ArrayList<String>();
            line = stderr.readLine();
            while (line != null) {
                stderr_lines.add(line);
                line = stderr.readLine();
            }
            if (p.waitFor() != 0) {
                System.err.println(StringUtils.join(stdout_lines, "\n"));
                System.err.println("-------------------------------------");
                System.err.println(StringUtils.join(stderr_lines, "\n"));
                throw new RuntimeException("javac failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        return abs_paths(find_classes(build.toString()));
    }

    static List<File> find_classes(String path) {
        File dir = new File(path);
        FindClasses fc = new FindClasses();
        try {
            Files.walkFileTree(Paths.get(path), fc);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't walk dir tree.");
        }
        return fc.files;
    }

    static List<String> abs_paths(List<File> files) {
        List<String> paths = new ArrayList<String>(files.size());
        for (File file : files) {
            paths.add(file.getAbsolutePath());
        }
        return paths;
    }

    /**
     * Takes the full package name of the java file to load from the
     * resources (see `Javac.load` for details). It uses `load` to get the
     * file and `javac` to compile it. It then loads up the classes using soot.
     *
     * @returns HashMap : package_name - JavaClass
     */
    public static HashMap<String, soot.SootClass> classes(String full_name) {
        String[] split = StringUtils.split(full_name, ".");
        String name = split[split.length-1];
        String basedir = "/tmp";
        javac(basedir, name, load(full_name));
        HashMap<String, soot.SootClass> map = new HashMap<String, soot.SootClass>();

        String cp = System.getenv().get("JAVA_JARS");
        Path dir = Paths.get(cwd).resolve(basedir);
        Path build = dir.resolve("build");
        String base_dir = build.toString();
        List<String> dirs = new ArrayList<String>();
        dirs.add(base_dir);

        soot.Scene S = edu.cwru.jpdg.JPDG.runSoot(cp, dirs);
        for (soot.SootClass klass : S.getApplicationClasses()) {
            map.put(klass.getName(), klass);
        }
        return map;
    }
}

class FindClasses extends SimpleFileVisitor<Path> {

    public List<File> files = new ArrayList<File>();

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            String ext = FilenameUtils.getExtension(file.toString());
            if (ext.equals("class")) {
                files.add(file.toFile());
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }
}

