# Initialising a fresh Neo4j database and importing some EAD

First, make sure there is no Neo4j server running that uses the graph DB you're about to build.

## Set Vital Environment var(s)

The **NEO4J_HOME** env var must be set and point to the Neo4j instance with the EHRI libs [installed](INSTALL.md).

A second var that _may_ be set is **NEO4J_DB**, which should point to the actual database directory. If not set,
this will default to `$NEO4J_HOME/data/graph.db`.

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
./scripts/cmd useradd mike --group admin
```

## Import some SKOS

To import some vocabulary terms you need to tell the script:

* The user doing the importing
* The vocabulary to import into (which can be created if necessary)

```bash
./scripts/cmd skos-import --createvocabulary --vocabulary ehri-terms --user mike /path/to/ehri-skos.rdf
```

## Import some EAD

The EAD importer is similar to above:

```bash
./scripts/cmd ead-import --createrepo --repo my-repo --user mike /path/to/ead-combined.xml
```

## TODO

Importing EAC/EAG
