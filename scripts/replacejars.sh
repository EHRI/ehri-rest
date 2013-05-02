#!/bin/bash
# Copy the jars needed for EHRI into the NEO4J system lib folder

NEO4JPATH=${1%}
# on my computer it is NEO4JPATH=/Users/paulboon/Documents/Development/neo4j-community-1.9.M05
if [ "$NEO4JPATH" == "" ]; then
    echo "Usage: $0 [NEO4J-HOME-PATH]"
    exit 2
fi
# Maybe check if it exists ?
NEO4JLIB=$NEO4JPATH/system/lib

# Run maven first to be sure we have the jars
echo "Attempting package..."
mvn test-compile package -DskipTests || { echo "Maven package exited with non-zero status, replace aborted..."; exit 4; }

# Note maybe we can ask mvn what the .m2 path is?
# but default is in the user home .m2
DOTM2REPO=$HOME/.m2/repository

###
# replace does a backup and then a copy
#
function replace(){
	local name=$1
	local version=$2
	local JARFILE=$name-$version.jar
	
	# backup any 'old' versions
	FILES=`find $NEO4JLIB -maxdepth 1 -type f -name "$name-*.jar" -not -name $JARFILE`
	for f in $FILES
	do
		echo "Rename $f to $f.bk"
		mv $f $f.bk
	done
	# copy 'new' version
	FILES=`find $DOTM2REPO -type f -name "$JARFILE"`
	numF=${#FILES[@]}
	case $numF in
		0) echo "File not found: $JARFILE";;
		1) 
			echo "Copying ${FILES[0]}"
			cp ${FILES[0]} $NEO4JLIB	
		;;
		*) echo "Found $numF similar jar files: NOT copying!"
			for f in $FILES
			do
				echo "Found $f"
			done
		;;
	esac
}

echo "=============="
echo "Replacing jars "
echo " - in $NEO4JLIB "
echo "   with jars "
echo " - from $DOTM2REPO" 
echo "=============="

replace frames 2.2.0
replace blueprints-core 2.2.0
replace blueprints-neo4j-graph 2.2.0
replace pipes 2.2.0
# some extra
replace gremlin-groovy 2.2.0
replace gremlin-java 2.2.0
# other stuff
replace guava 14.0
replace joda-time 2.1
replace commons-cli 1.2
replace snakeyaml 1.11
replace opencsv 2.3

echo "Done."
