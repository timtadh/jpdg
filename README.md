# jpdg

By Tim Henderson (tim.tadh@gmail.com)

# What?

Nothing yet, but you can build all the submodules with `buildr`

# Downloading

You can download a [prebuilt jar](https://github.com/timtadh/jpdg/releases) for
the current release.

# Building

Before you get started you will need Java 7 and [Apache Buildr
1.4.15](buildr.apache.org). Once you have those it should be as straight
forward as:

    git clone git@github.com:timtadh/jpdg.git
    cd jpdg
    git submodule init
    git submodule update
    buildr soot:package
    buildr run

Soot should be rebuilt any time the submodules change.


