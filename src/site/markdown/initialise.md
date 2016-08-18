# Initialising a fresh Neo4j database and importing some data

First, make sure there is no Neo4j server running that uses the graph DB you're about to build.

## Set Vital Environment var(s)

The **NEO4J_HOME** env var must be set and point to the Neo4j instance with the EHRI libs [installed](INSTALL.md).

A second var that _may_ be set is **NEO4J_DB**, which should point to the actual database directory. If not set,
this will default to `$NEO4J_HOME/data/databases/graph.db`.

## Make sure the Neo4j Server is stopped

At present, the various commands all use Neo4j in embedded mode. This will not work if the server is running,
because only one process can write to a graph DB at once. So before running anything, make sure the server is stopped:

**NB**: Also note that the install script, in an effort to be helpful, starts the server at the end.

```bash
$NEO4J_HOME/bin/neo4j stop
```

## Initialise the DB

Initialisation creates some nodes that are essential to the EHRI environment:

* The global event root node
* The admin group
* Permission type nodes
* Content type nodes

```bash
./scripts/cmd initialize
```

## Create a user account

So you can do stuff within the system, you need to make a yser profile for yourself. Probably you also want to add that profile to the admin group, which can be accomplished like so:

```bash
./scripts/cmd useradd $USER --group admin
```

## Verify existence of the new account

You should now be able to verify that the new account exists with the command:

```bash
./scripts/cmd list UserProfile
```