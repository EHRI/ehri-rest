# Example Ingest

The current ingest procedure is somewhat long-winded and technical. This is an example
given a single EAD XML file containing a large number (48,000) of individual documentary
unit items in a single fonds. The repository is the Internation Tracing Service (ITS),
which has EHRI repository ID `de-002409`.

This ingest covers importing the EAD file into the staging server, at which time it
should be ready for verification and if necessary, changes, before the production
ingest.

## Before you start

First, log into the EHRI staging server via SSH and open a bunch of shells.
In one of them, tail the following file, which will give us some information
about what goes wrong, when something inevitably goes wrong the first few times
we try:

    tail -f /opt/webapps/neo4j-version/data/log/console.log

### Back up the database

The Neo4j DB lives in /opt/webapps/data/graph.db. You can back it up without shutting 
down the server by running:

    /opt/webapps/neo4j-backup.sh graph.db.BAK

To restore the DB the procedure is:
 - shut down Neo4j
 - replace /opt/webapps/data/graph.db with backup directory you specified previously
 - ensure all files in the graph.db directory are owned and writable by the `webadm` group:
    - chgrp -R webadm graph.db
    - chmod -R g+rw graph.db
 - restart Neo4j

## Procedure

Onwards with the ingest...

Next, in another shell, copy the file(s) to be ingested to the server and place them
in `/opt/webapps/data/import-data/de/de-002409`, the working directory for ITS data.

Import properties handle certain mappings between tags (with particular attributes)
and EHRI fields. The ITS data has a particular mapping indicating that when the
`<unitid>` has a `type="refcode"` that is the main doc unit identifier, and that the
rest are the alternates. This file is, in this case:

    /opt/webapps/data/import-data/de/de-002409/its-pertinence.properties

The actual import is done via the /ehri/import/ead endpoint on the Neo4j extension. It is
documented here: http://ehri.github.io/docs/api/ehri-rest/ehri-extension/wsdocs/resource_ImportResource.html

The basic procedure is:

 - obtain an appropriate import properties file (which we've done in this case)
 - write an appropriate log file, describing what we're doing
 - stick the EAD XML on the server
 - run a curl command, POSTing the XML data to the ingest endpoint, with
   the appropriate parameters
 - re-index the data held by our repository (ITS, de-002409) to make it
   searchable in the portal UI

To make the curl command less cumbersome, lets export the path to the properties
file as an environment variable:

    export PROPERTIES=/opt/webapps/data/import-data/de/de-002409/its-pertinence.properties

Also, lets write a log file and export it's path as an environment variable:

    echo "Importing ITS data with properties: $PROPERTIES" > LOG.txt
    export LOG=`pwd`/LOG.txt

Now we can POST the data to the ingest endpoint:

    curl -XPOST \
        -H "X-User:mike" \
        -H "Content-type: text/xml" \
        --data-binary @KHSK_GER.xml \
        "http://localhost:7474/ehri/import/ead?scope=de-002409&log=$LOG&properties=$PROPERTIES"

These parameters are:

 - the `X-User` header tells the web service which user is responsible for the ingest
 - the `Content-type` header tells it to expect XML data
 - the `scope=de-002409` query parameter tells it we're importing this EAD into
   the ITS repository
 - the `log=$LOG` parameter tells it to find the log text in a local file
 - the `properties=$PROPERTIES` parameter tells it to file the import properties
   in a local file

**Note**: when importing a single EAD containing ~50,000 items in a single transaction the
staging server might run out of memory. If it does the only option is to increase the
Neo4j heap size  by uncommenting and setting the `wrapper.java.maxmemory=MORE_MB` (say,
 3500) in `$NEO4J_HOME/conf/neo4j-wrapper.conf` and restarting Neo4j by running:

    sudo service neo4j-service restart

**Additional note**: _Certain date patterns are fuzzy parsed by the importer and invalid
dates such as 31st April will currently throw a runtime exception resulting in a BadRequest
from the web service. So fix all these first_ ;)

If all goes well you should get something like this:

    {"created":48430,"unchanged":0,"message":"Import ITS 0.4 data using its-pertinence.properties.\n","updated":0,"errors":{}}

In theory, that ingest should be idemotent, so you can run the same command again and not change anything. Instead you'd
get a reply like:

    {"created":0,"unchanged":48430,"message":"Import ITS 0.4 data using its-pertinence.properties.\n","updated":0,"errors":{}}

## Indexing

The final step is the re-index the ITS repository, making the items searchable. This can be done
from the Portal Admin UI, or via the following command:

    java -jar /opt/webapps/docview/bin/indexer.jar \
         --clear-key-value holderId=de-002409 \
         --index -H "X-User=admin" \
         --stats \
         --solr http://localhost:8080/ehri/portal \
         --rest http://localhost:7474/ehri \
         "Repository|de-002409"

(This tool is a library/CLI utility the is used by the portal UI and available on the server: see
the https://github.com/EHRI/ehri-search-tools project for more details.)

## Updating existing collections

To update existing collections, when, for example, adding descriptions in another language, the
procedure is exactly the same with one exception: the import Curl command needs an additional 
parameter:

    &allow-update=true

Without this parameter the importer will throw a mode violation error when it ends up updating
an existing collection.

## Ingesting multiple files in an archive

It is possible to ingest multiple EAD files in a single transaction by providing the importer
with an archive file (containing multiple XML files) instead of a single XML file. Currently
the following formats are supported:

 - zip (although some extensions are problematic)
 - tar
 - tar.gz

The importer will assume the data it is given is an archive if the content type of the
request is given as `application/octet-stream` (aka, miscellaneous binary) instead of
either `text/xml` (XML) or `text/plain` (local file paths.)

**Note**: if several EAD files provide different translations of the same items it is
necessary to enable update ingests via `&allow-updates=true`.