#!/bin/sh

echo "Initializing DB: $NEO4J_HOME/data/databases/graph.db"

$NEO4J_HOME/scripts/cmd initialize
$NEO4J_HOME/scripts/cmd gen-schema

ADMIN_USER=${ADMIN_USER:-""}

if [ "$ADMIN_USER" != "" ]; then
    echo "Adding administrative user: $ADMIN_USER"
    $NEO4J_HOME/scripts/cmd useradd $ADMIN_USER --group admin
fi

chown neo4j.neo4j -R $NEO4J_HOME/data/databases
