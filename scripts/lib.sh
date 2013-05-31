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

    # Default the graph db directory to $NEO4J_HOME/data/graph.db unless
    # NEO4J_DB is explicitly set
    if [ "$NEO4J_DB" = "" ]; then
        export NEO4J_DB=$NEO4J_HOME/data/graph.db
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
    exit 1
  fi

  # confirm system jars
  SYSLIBDIR="$NEO4J_HOME"/system/lib
  if [ ! -e "$SYSLIBDIR" ] ; then
    echo "Error: missing Neo4j System Library, expected at $SYSLIBDIR"
    exit 1
  fi

  # confirm system jars
  EHRILIBDIR="$NEO4J_HOME"/plugins/ehri
  if [ ! -e "$EHRILIBDIR" ] ; then
    echo "Error: missing EHRI libs, expected at $EHRILIBDIR"
    exit 1
  fi

  ALL_JARS=""
  for jar in "$LIBDIR"/*.jar "$SYSLIBDIR"/*.jar "$EHRILIBDIR"/*.jar ; do
    [ -z "$ALL_JARS" ] && ALL_JARS="$jar" || ALL_JARS="$ALL_JARS":"$jar"
  done

  CLASSPATH=${ALL_JARS}
  #echo "CLASPATH: $CLASSPATH"
  # add useful conf stuff to classpath - always a good idea
  CLASSPATH="$CLASSPATH":"$NEO4J_HOME"/conf/
}


