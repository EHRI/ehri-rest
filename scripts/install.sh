#!/bin/bash

#
# Install the EHRI libs into a Neo4j instance. The (optional)
# single argument is the NEO4J_HOME, which otherwise defaults
# to the env var $NEO4J_HOME
#

NEO4JPATH=${1%}

# Default to NEO4J_HOME
if [ "$NEO4PATH" == "" ]; then
    NEO4JPATH=$NEO4J_HOME
fi

if [ ! -e $NEO4JPATH -o ! -d $NEO4JPATH ]; then
    echo "Neo4j path '$NEO4JPATH' does not exist, or is not a directory"
    exit 2
fi


if [ ! -e $NEO4JPATH/plugins -o ! -e $NEO4JPATH/system/lib ]; then
    echo "Cannot detect 'plugins' or 'system/lib' directories in '$NEO4JPATH'. Are you sure this is the right dir?"
    exit 2
fi

# Run maven to package our stuff...
echo "Attempting package..."
mvn clean package -DskipTests || { echo "Maven package exited with non-zero status, install aborted..."; exit 4; }

# find archive and untar it...
archive=`ls assembly/target/assembly*tar.gz`
if [ "$archive" == "" ]; then
    echo "Error: archive not found in dist/target... aborting..."
    exit 3
fi

echo "Extracting file: $archive"
outpath=$NEO4JPATH/plugins/ehri
if [ -e $outpath ]; then
    rm $outpath/*jar
    rmdir --ignore-fail-on-non-empty $outpath
fi
mkdir -p $outpath
tar -C $outpath -zxvf $archive

echo "EHRI libs installed..."

echo
echo "IMPORTANT: You must manually ensure the $NEO4JPATH/conf/neo4j-server.properties configuration contains the line:"
echo "   org.neo4j.server.thirdparty_jaxrs_classes=eu.ehri.extension=/ehri"
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


