#!/bin/bash

FILE="${BASH_SOURCE[0]}"

# The first argument to the script must be a directory containing the EAD files.
# The second argument is the username to run the import as.
if (( $# != 3 )); then
    echo "Usage: import-ushmm.sh <directory to import EADs from> <username in graph> <log message>"
    exit 1
fi

IMPORTDIR="$1/"
USERNAME="$2"
LOGMESSAGE="$3"

# listing all USHMM XML files is too long. First create file, then check for XML.
# Debug
echo "$IMPORTDIR"

# Go to this script file's directory
cd $(dirname $FILE)

# List the directory contents into a file
ls $IMPORTDIR > "allushmmfiles"
head allushmmfiles

# Check that the directory contains XML files
# if [[ $(  ) -ne "" ]]; then
#     echo "Your directory has no XML files."
#     exit 1
# fi

NUMALLLINES=$( wc -l allushmmfiles )
echo "$NUMALLLINES files found in $IMPORTDIR"

EIMPORTDIR=$( echo "$IMPORTDIR" | sed -e "sp\/\/p\/pg" | sed -e "sp\/p\\\/pg" | sed -e "s/[^\/]$/&\/$/" )
SEDCOM="/^.*\.xml$/s//$EIMPORTDIR&/p"

# Debug
echo $SEDCOM

# Debug
head allushmmfiles | sed -n -e "$SEDCOM" 

sed -n -i ".bak" -e "$SEDCOM" allushmmfiles


echo "$( wc -l allushmmfiles ) XML files found in $IMPORTDIR"

# Split directory listing into multiple files with non-colliding names
split allushmmfiles "ushmmsplitfile-"

# Debug: show created files
echo "Going to import the XMLs (as $USERNAME) listed in:"
ls ushmmsplitfile-*

# Import each of the split files.
for F in $( ls ushmmsplitfile-* ); do
    ./cmd ushmm-ead-import -user $USERNAME -scope us-005578 -log "$LOGMESSAGE" -F "$F"
done

# Clean up split files? Keep allushmmfiles for logs.
echo "Removing XML split list files..."
rm ushmmsplitfile-*

# Go back to pwd
cd $(pwd)
