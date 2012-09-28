#!/bin/sh
# Load an EAD file

ARGS="$@"

mvn -Dexec.classpathScope=compile \
    -pl ehri-cmdline \
    -Dexec.mainClass="eu.ehri.project.commands.CmdEntryPoint" \
    -Dlog4j.rootLogger=WARN \
    -quiet \
    -Dexec.args="$ARGS" \
    exec:java
