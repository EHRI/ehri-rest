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
echo "Creating user: $USER"
./scripts/cmd $DB useradd $USER -group admin
echo "Importing Wiener Library EAD..."
./scripts/cmd $DB ead-import --createrepo -repo wiener-library -user $USER -tolerant ~/Dropbox/EHRI-WP19-20/TestData/wiener-library/*xml
echo "Importing ICA-AtoM EAC authorities..."
./scripts/cmd $DB eac-import -repo wiener-library -user $USER -tolerant ~/Dropbox/EHRI-WP19-20/TestData/eac-dump-140313/*xml
echo "Importing ICA-AtoM EAG institutions..."
./scripts/cmd $DB eag-import -repo wiener-library -user $USER -tolerant ~/Dropbox/EHRI-WP19-20/TestData/eag-dump-080313/*xml
