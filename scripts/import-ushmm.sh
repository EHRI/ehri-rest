#!/bin/bash

FILE="${BASH_SOURCE[0]}"

# pwd
cd $(dirname $FILE)
for F in $(ls /Users/ben/Documents/Projecten/EHRI/ushmm/ead/x*); do
    ./cmd ushmm-ead-import -user benc -scope us-005578 -F "$F"
done
cd $(pwd)