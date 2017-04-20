#!/bin/sh

set -e

echo "Initializing DB: $NEO4J_HOME/data/databases/graph.db"

$NEO4J_HOME/scripts/cmd initialize
$NEO4J_HOME/scripts/cmd gen-schema

if [ ! -z "$ADMIN_USER" ]; then
    echo "Adding administrative user: $ADMIN_USER"
    $NEO4J_HOME/scripts/cmd useradd $ADMIN_USER --group admin
fi

