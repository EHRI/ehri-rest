Loading JSON Data Dumps
=======================

A common scenario is exporting data from a production DB to a local testing instance. There are a
few ways to do this:

 - using a Neo4j hot backup of the `graph.db` directory
 - using a GraphSON dump
 

## Exporting GraphSON into a local instance

First, open an SSH port-forward to the local machine, e.g:

    ssh [EHRISERVER] -L7777:localhost:7474

Neo4j typically runs on port 7474, so the remote instance will be available on port 7777 on the 
local machine.

Now, make sure the local Neo4j instance is stopped and that the local `graph.db` directory is
either non-existent or completely empty. We can then combine the `export-graphson` web service
method with the `graphson` command-line tool to pipe data from the (online) remote instance to
the (offline) local one line so:

    curl -H "X-User:admin" http://localhost:7777/ehri/admin/export-graphson | ./scripts/cmd graphson --load - 
    --buffer-size 100000 && ./scripts/cmd gen-schema

The options are:

 - `--load -` (load from stdin)
 - `--buffer-size 10000` (flush the TX every 100000 primitives loaded, to avoid running out of memory)
 
The follow on command `./scripts/cmd gen-schema` is necessary to instantiate the Neo4j indexes
and constraints.