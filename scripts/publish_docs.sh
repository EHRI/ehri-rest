#!/bin/sh

set -e

DOC_REPO_PATH=$1
DOC_PATH=docs/api/ehri-rest

if [ -z "$DOC_REPO_PATH" ]; then
    echo "usage: public_docs.sh <doc-repository-path>"
    exit 1
else
    if [ ! -e "${DOC_REPO_PATH}/${DOC_PATH}" ]; then
        echo "Doc path $DOC_PATH not found in repository; are you sure this is the right dir?"
        exit 2
    fi
fi

mvn site
mvn site:stage
rsync -avl target/staging/ "${DOC_REPO_PATH}/${DOC_PATH}/"

(
    cd $DOC_REPO_PATH
    git add $DOC_PATH
    git ci -m "Updating backend Java and web service docs" $DOC_PATH
    git push
)

exit 0

