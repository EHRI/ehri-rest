#!/bin/sh

VERSION=$($NEO4J_HOME/scripts/cmd version 2>&1)
echo "EHRI version: $VERSION"

NEO4J_DB="$NEO4J_HOME/data/databases/graph.db"
if [ ! -d "$NEO4J_DB" ]; then
  echo "Initializing DB: $NEO4J_DB"
  $NEO4J_HOME/scripts/cmd initialize
  $NEO4J_HOME/scripts/cmd gen-schema
fi

ADMIN_USER=${ADMIN_USER:-""}

if [ "$ADMIN_USER" != "" ]; then
    echo "Adding administrative user: $ADMIN_USER"
    $NEO4J_HOME/scripts/cmd useradd $ADMIN_USER --group admin
fi

chown neo4j.neo4j -R $NEO4J_HOME/data/databases
