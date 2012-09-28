#!/bin/sh
# Load an EAD file

echo "Running '$@'"
ARGS="$@"

mvn -Dexec.classpathScope=test \
    -pl ehri-importers \
    -Dexec.mainClass="eu.ehri.project.importers.EadImportManager" \
    -Dexec.args="$ARGS" \
    exec:java
