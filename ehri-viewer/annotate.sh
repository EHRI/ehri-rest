#!/bin/bash

# NOTE: use chmod 755 to make it executable

NEO4J_PORT="7474"

echo ""
echo "Annotation using curl, be prepared for a bit rough ride..."
echo "Assuming you have neo4j running on \"localhost:$NEO4J_PORT\"" 
echo ""

### NO INPUT VALIDATION ###

read -p "Enter annotator (userProfile) id: "
ANNOTATOR_ID=$REPLY

read -p "Enter annotation target id: "
TARGET_ID=$REPLY

read -p "Enter annotation body text: "
BODY=$REPLY

echo    "==========================="
read -p "Continue creating annotation (y/n)?"
if [ "$REPLY" != "y" ] ; then
  echo "Canceled annotation creation"
  echo ""
  exit
fi

echo ""
echo "Start annotation creation using" 
echo " - annotator: $ANNOTATOR_ID"
echo " - hasTarget: $TARGET_ID" 
echo " - with body: \"$BODY\""
echo ""

## Create the annotation node
echo ""
echo "Creating annotation node..."
echo ""
ID=$(curl --silent POST -H "Content-type: application/json" http://localhost:$NEO4J_PORT/db/data/node -d'{"isA": "annotation","body": "'$BODY'"}' | grep -Eo '"self" : "http://.*",' | sed -E 's/.*\/node\/([0-9]*).*/\1/')
echo ""
echo "Annotation id: $ID" 
## Create edge from annotation to target
echo ""
echo "Creating edge from annotation to target..."
echo ""
curl --silent POST -H "Content-type: application/json" http://localhost:$NEO4J_PORT/db/data/node/$ID/relationships -d'{"to": "'$TARGET_ID'","type": "annotates"}'
## Create edge from annotator to annotation
echo "Creating edge from annotator to annotation..."
curl --silent POST -H "Content-type: application/json" http://localhost:$NEO4J_PORT/db/data/node/$ANNOTATOR_ID/relationships -d'{"to": "'$ID'","type": "hasAnnotation"}'
echo ""
echo ""
echo "Done with annotation"
echo ""
