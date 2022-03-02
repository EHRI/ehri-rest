#!/bin/bash

#
# Install the EHRI libs into a Neo4j instance. The (optional)
# single argument is the NEO4J_HOME, which otherwise defaults
# to the env var $NEO4J_HOME
#

NEO4JPATH=${1%}

# Default to NEO4J_HOME
if [ "$NEO4JPATH" == "" ]; then
    NEO4JPATH=$NEO4J_HOME
fi

if [ ! -e $NEO4JPATH -o ! -d $NEO4JPATH ]; then
    echo "Neo4j path '$NEO4JPATH' does not exist, or is not a directory"
    exit 2
fi


if [ ! -e $NEO4JPATH/plugins -o ! -e $NEO4JPATH/data ]; then
    echo "Cannot detect 'plugins' or 'data' directories in '$NEO4JPATH'. Are you sure this is the right dir?"
    exit 2
fi

# Run maven to package our stuff...
echo "Attempting package..."
mvn clean package -DskipTests || { echo "Maven package exited with non-zero status, install aborted..."; exit 4; }

# find archive and untar it...
jar=`ls build/target/ehri-data-*.jar`
if [ "$jar" == "" ]; then
    echo "Error: uberjar not found in build/target... aborting..."
    exit 3
fi

outpath=$NEO4JPATH/plugins
if [ -e $outpath ]; then
    rm $outpath/ehri-data-*.jar
fi
cp $jar $outpath

echo "EHRI lib installed..."

echo
echo "IMPORTANT: You must manually ensure the $NEO4JPATH/conf/neo4j.conf configuration contains the line:"
echo "   dbms.unmanaged_extension_classes=eu.ehri.project.ws=/ehri"
echo


# Restart server...
echo "Restart server?"
select yn in "Yes" "No"; do
    case $yn in
        Yes ) $NEO4JPATH/bin/neo4j restart ; break;;
        No ) break;;
    esac
done

exit 0


