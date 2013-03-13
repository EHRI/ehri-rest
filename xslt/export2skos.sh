#!/bin/bash

# Export the vocabulary in the graph database 
# to SKOS-RDF (an XML file). 
###

# TODO use input parameters instead of the defined vars? 
NEO4J_DBDIR=/Users/paulboon/Documents/Development/neo4j-community-1.9-SNAPSHOT-EHRI/data/graph.db
EHRI_PATH=/Users/paulboon/Documents/workspace/neo4j-ehri-plugin
SAXONJAR_PATH=/Applications/oxygen/lib
OUTPUT_PATH=/Users/paulboon/Desktop/EHRI/scripts
XSL_PATH=/Users/paulboon/Desktop/EHRI/scripts

# Get all concepts in an xml file
# TODO put timestamp in filename
#      - not using mvn but calling java -jar ... would be simpler?
echo Extracting concepts...
cd $EHRI_PATH;
./scripts/cmd $NEO4J_DBDIR list cvocConcept --format xml > $OUTPUT_PATH/concepts.xml
echo 

# transform with xslt, could go in previous step... but for now use extracted xml. 
# xsltproc on OSX, but what is it on the server?
# use saxon: java -jar /path/to/saxon.jar xmlfile xslfile
# 
INPUT_FILE=$OUTPUT_PATH/concepts.xml
OUTPUT_FILE=$OUTPUT_PATH/concepts.rdf
XSL_FILE=$XSL_PATH/cvocConcept_skos.xsl
echo Transforming $INPUT_FILE to $OUTPUT_FILE ...
java -jar $SAXONJAR_PATH/saxon.jar $INPUT_FILE $XSL_FILE > $OUTPUT_FILE
echo 

echo Finished exporting. 
