#!/bin/bash

#
# Install the EHRI libs into a Neo4j instance. One and only
# argument should specify the path the the NEO4JPATH which
#

BLUEPRINTS_VERS="2.1.0"
BLUEPRINTS_DEPS=( frames blueprints-core blueprints-neo4j-graph pipes )
EXTRA_DEPS=( guava-10.0.1.jar joda-time-2.1.jar )

if [ $# -ne 1 ]; then
    echo "Usage: `basename $0` <NEO4JPATH>"
    exit 1
fi

NEO4JPATH=$1

if [ ! -e $NEO4JPATH -o ! -d $NEO4JPATH ]; then
    echo "Neo4j path '$NEO4JPATH' does not exist, or is not a directory"
    exit 2
fi


if [ ! -e $NEO4JPATH/plugins -o ! -e $NEO4JPATH/system/lib ]; then
    echo "Cannot detect 'plugins' or 'system/lib' directories in '$NEO4JPATH'. Are you sure this is the right dir?"
    exit 2
fi

# Check blueprints dependencies
FLAG=0
for dep in ${BLUEPRINTS_DEPS[@]}; do
    jar=${dep}-${BLUEPRINTS_VERS}.jar
    if [ ! -e $NEO4JPATH/system/lib/$jar ] ; then
        echo "Missing dependency: '$jar'. This must manually be installed in $NEO4JPATH/system/lib."
        FLAG=1
    fi
done
for dep in ${EXTRA_DEPS[@]}; do
    if [ ! -e $NEO4JPATH/system/lib/$dep ]; then
        echo "Missing dependency: '$dep'. This must manually be installed in $NEO4JPATH/system/lib."
        FLAG=1
    fi
done


if [ $FLAG -eq 1 ] ; then
    echo "Missing manual dependencies."
    exit 3
fi

echo "Attempting package..."
mvn clean test-compile package -DskipTests || { echo "Maven package exited with non-zero status, install aborted..."; exit 4; }

PLUGINJAR=`ls ehri-plugin/target/ehri-plugin*jar|grep -v test`
EXTENSIONJAR=`ls ehri-extension/target/ehri-extension*jar|grep -v test`
FRAMESJAR=`ls ehri-frames/target/ehri-frames*jar|grep -v test` 

for jar in $FRAMESJAR $EXTENSIONJAR $PLUGINJAR; do
    if [ $jar == '' ]; then
        echo "Unable to find all jars, check build is correct."
        exit 5
    fi
done

echo "Copying $PLUGINJAR to $NEO4JPATH/plugins" 
cp ehri-plugin/target/ehri*jar $NEO4JPATH/plugins
echo "Copying $EXTENSIONJAR to $NEO4JPATH/plugins" 
cp ehri-extension/target/ehri*jar $NEO4JPATH/plugins
echo "Copying $EXTENSIONJAR to $NEO4JPATH/system/lib" 
cp ehri-extension/target/ehri*jar $NEO4JPATH/system/lib
echo "Copying $FRAMESJAR to $NEO4JPATH/system/lib" 
cp ehri-frames/target/ehri*jar $NEO4JPATH/system/lib

# Restart server...
echo "Restarting server..."
$NEO4JPATH/bin/neo4j restart

echo
echo "IMPORTANT: You must manually ensure the $NEO4JPATH/conf/neo4j-server.properties configuration contains the line:"
echo "   org.neo4j.server.thirdparty_jaxrs_classes=eu.ehri.extension=/ehri"
echo

exit 0
