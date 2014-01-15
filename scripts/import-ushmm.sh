#!/bin/bash

FILE="${BASH_SOURCE[0]}"

if (( $# != 1 )); then
    echo "Don't forget to add a directory."
    exit 1
fi

IMPORTDIR="$1"

if [[ $( ls "$IMPORTDIR/*.xml" ) -ne "" ]]; then
    echo "Your directory has no XML files."
    exit 1
fi

echo "$IMPORTDIR"

# pwd
# cd $(dirname $FILE)
# for F in $(ls /Users/ben/Documents/Projecten/EHRI/ushmm/ead/x*); do
#     ./cmd ushmm-ead-import -user benc -scope us-005578 -F "$F"
# done
# cd $(pwd)