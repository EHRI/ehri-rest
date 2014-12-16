#!/bin/bash

# Import a large number of EAD files in small batches

# Usage: import-large-batch.sh <directory of files> <scope> <log message file> <properties file> <handler to use>

DATADIR=$1
SCOPE=$2
LOGFILE=$3
PROPERTIES=$4
HANDLER=$5

for FILE in `ls /opt/webapps/data/import-data/$DATADIR`; do
    echo "/opt/webapps/data/import-data/$DATADIR/$FILE" >> /opt/webapps/data/import-metadata/$SCOPE-$DATADIR.txt
done
split -l 100 /opt/webapps/data/import-metadata/$SCOPE-$DIR.txt /opt/webapps/data/import-metadata/$SCOPE-split-$DIR-

for TFILE in `ls /opt/webapps/data/import-metadata/$SCOPE-split*`; do
    echo "importing $TFILE..."
    head $TFILE
    curl -X POST -m 7200 -H "Authorization: $USER" --data-binary @$TFILE -H "Content-Type: text/plain" "http://localhost:7474/ehri/import/ead?scope=$SCOPE&log=$LOGFILE&&properties=$PROPERTIES&handler=$HANDLER"    
done
