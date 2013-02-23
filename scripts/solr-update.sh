#!/bin/sh

curl http://localhost:8983/solr/portal/update?commit=true -H "Content-Type: text/xml" --data @$1
