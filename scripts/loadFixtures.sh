#!/bin/sh
# Load the fixtures into the Neo4j data specified by the first argument.
mvn -Dexec.classpathScope=test \
    -pl ehri-frames        \
    -Dexec.mainClass="eu.ehri.project.test.utils.FixtureLoader" \
    -Dexec.args="$@" \
    exec:java
