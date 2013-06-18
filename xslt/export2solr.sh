#!/bin/bash

# Export the vocabulary in the graph database 
# to Solr (an XML file) and update the index. 
###

# TODO use input parameters instead of the defined vars? 
NEO4J_DBDIR=/Users/paulboon/Documents/Development/neo4j-community-1.9-SNAPSHOT-EHRI/data/graph.db
EHRI_PATH=/Users/paulboon/Documents/workspace/neo4j-ehri-plugin
SAXONJAR_PATH=/Applications/oxygen/lib
OUTPUT_PATH=/Users/paulboon/Desktop/EHRI/scripts
INPUT_PATH=/Users/paulboon/Desktop/EHRI/scripts
XSL_PATH=/Users/paulboon/Desktop/EHRI/scripts

# Get all documentaryUnit's in an xml file
# TODO put timestamp in filename
#      - not using mvn but calling java -jar ... would be simpler?
echo Extracting documentaryUnits...
cd $EHRI_PATH;
./scripts/cmd $NEO4J_DBDIR list documentaryUnit --format xml > $OUTPUT_PATH/documents.xml
echo

# transform with xslt, could go in previous step... but for now use extracted xml. 
# xsltproc on OSX, but what is it on the server?
# use saxon: java -jar /path/to/saxon.jar xmlfile xslfile
# 
INPUT_FILE=$INPUT_PATH/documents.xml
OUTPUT_FILE=$OUTPUT_PATH/documents_solr.xml
XSL_FILE=$XSL_PATH/documentaryUnit_solr.xsl
echo Transforming $INPUT_FILE to $OUTPUT_FILE ...
java -jar $SAXONJAR_PATH/saxon.jar $INPUT_FILE $XSL_FILE > $OUTPUT_FILE
echo

# update in Solr using curl
#SOLR_URL=http://localhost:8080/solr-example
SOLR_URL=http://localhost:8080/solr-ehri/registry
f=$OUTPUT_FILE
# post new stuff
echo Posting file $f to $SOLR_UPDATE_URL ...
curl $SOLR_URL/update --data-binary @$f -H 'Content-type:application/xml' 
echo

# send the commit command to make sure all the changes are flushed and visible
echo Commit changes ...
curl $SOLR_URL/update --data-binary '<commit/>' -H 'Content-type:application/xml'
echo

echo Finished exporting. 
