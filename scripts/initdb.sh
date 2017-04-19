#!/bin/sh

set -e

echo "Initializing DB: $NEO4J_HOME/data/databases/graph.db"

$NEO4J_HOME/scripts/cmd initialize
$NEO4J_HOME/scripts/cmd gen-schema

