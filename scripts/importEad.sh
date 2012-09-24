#!/bin/sh
# Load an EAD file
mvn -Dexec.classpathScope=test \
    -pl ehri-importers \
    -Dexec.mainClass="eu.ehri.project.importers.EadImporter" \
    -Dexec.args="$@" \
    exec:java
