#!/bin/sh

DB=${1%/}

if [ "$DB" == "" ]; then
    echo "Usage: builddb.sh [NEO4J-DATABASE-PATH]"
    exit 2
fi

if [ -e $DB ]; then
    if [ ! -d $DB ]; then
        echo "Error: $DB is not a directory!"
        exit 2
    fi
fi

echo "Importing SKOS EHRI Thesaurus..."
./scripts/cmd $DB skos-import --createvocabulary -vocabulary ehri-thes -user admin -tolerant ~/Dropbox/EHRI-WP19-20/TestData/ehri-skos.rdf
