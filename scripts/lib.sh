#!/bin/sh

checkenv() {
    # Build the class path
    if [ "$NEO4J_HOME" = "" ]; then
        echo "NEO4J_HOME not set; this should point to the Neo4j instance in which the EHRI libs are installed."
        exit 1
    else
        # strip any trailing slash
        export NEO4J_HOME=${NEO4J_HOME%/}
    fi

    # Default the graph db directory to $NEO4J_HOME/data/databases/neo4j unless
    # NEO4J_DB is explicitly set
    if [ "$NEO4J_DB" = "" ]; then
        export NEO4J_DB=$NEO4J_HOME/data/databases/neo4j
    else
        export NEO4J_DB=${NEO4J_DB%/}
        if [ -e $NEO4J_DB ] && [ ! -d $NEO4J_DB ]; then
            echo "Error: NEO4J_DB: '$NEO4J_DB' is not a directory!"
            exit 1
        fi
    fi
}

buildclasspath() {
  # confirm library jars
  LIBDIR="$NEO4J_HOME"/lib
  if [ ! -e "$LIBDIR" ] ; then
    echo "Error: missing Neo4j Library, expected at $LIBDIR"
  fi

  # confirm system jars
  PLUGINDIR="$NEO4J_HOME"/plugins
  if [ ! -e "$PLUGINDIR" ] ; then
    echo "Error: missing EHRI libs, expected at $PLUGINDIR"
  fi

  ALL_JARS=""
  for jar in "$LIBDIR"/*.jar "$PLUGINDIR"/*.jar ; do
    [ -z "$ALL_JARS" ] && ALL_JARS="$jar" || ALL_JARS="$ALL_JARS":"$jar"
  done

  CLASSPATH=${ALL_JARS}
  #echo "CLASPATH: $CLASSPATH"
  # add useful conf stuff to classpath - always a good idea
  CLASSPATH="$CLASSPATH":"$NEO4J_HOME"/conf
}


