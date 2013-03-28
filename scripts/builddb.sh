#!/bin/bash

DB=${1%/}
PROF=$2

# If second arg not given, use $USER
if [ -z "$PROF" ]; then
    PROF=$USER
fi
echo "Proceeding with user: $PROF"

if [ "$DB" == "" ]; then
    echo "Usage: builddb.sh [NEO4J-DATABASE-PATH]"
    exit 2
fi

if [ -e $DB ]; then
    if [ ! -d $DB ]; then
        echo "Error: $DB is not a directory!"
        exit 2
    fi
    echo "Neo4j database \"$DB\" already exists. Continue?"
    select yn in "Yes" "No" "Backup"; do
        case $yn in
            Yes ) break;;
            No ) exit;;
            Backup ) tmp=$DB-`date +"%Y%m%d%H%M%S"`; mv $DB $tmp ; echo "DB backed up to $tmp..."; break;;
        esac
    done
fi

echo "Initializing..."
./scripts/cmd $DB initialize
echo "Creating user: $PROF"
./scripts/cmd $DB useradd $PROF -group admin
echo "Importing Wiener Library EAD..."
./scripts/cmd $DB ead-import --createrepo -repo wiener-library -user $PROF -tolerant ~/Dropbox/EHRI-WP19-20/TestData/wiener-library/*xml
echo "Importing ICA-AtoM EAC authorities..."
./scripts/cmd $DB eac-import -repo wiener-library -user $PROF -tolerant ~/Dropbox/EHRI-WP19-20/TestData/eac-dump-140313/*xml
echo "Importing ICA-AtoM EAG institutions..."
./scripts/cmd $DB eag-import -repo wiener-library -user $PROF -tolerant ~/Dropbox/EHRI-WP19-20/TestData/eag-dump-080313/*xml
echo "Importing thesaurus..."
./scripts/cmd $DB skos-import --createvocabulary -vocabulary ehri-skos -user $PROF -tolerant ~/Dropbox/EHRI-WP19-20/TestData/ehri-skos.rdf

