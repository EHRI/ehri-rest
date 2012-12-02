#!/bin/sh

mvn -Dexec.args=$1 -pl ehri-frames -Dexec.classpathScope=test -Dexec.mainClass=eu.ehri.project.test.utils.fixtures.impl.YamlFixtureLoader exec:java
