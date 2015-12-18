#!/bin/sh

DB=$1
shift

mvn -Dexec.args=$DB -pl ehri-core -Dexec.classpathScope=test \
-Dexec.mainClass=eu.ehri.project.test.utils.fixtures.impl.YamlFixtureLoader $@ exec:java
