# Initialising a fresh Neo4j database and importing some EAD

First, make sure there is no Neo4j server running that uses the graph DB you're about to build.

## Set Vital Environment var(s)

The **NEO4J_HOME** env var must be set and point to the Neo4j instance with the EHRI libs [installed](INSTALL.md).

A second var that _may_ be set is **NEO4J_DB**, which should point to the actual database directory. If not set,
this will default to `$NEO4J_HOME/data/graph.db`.

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

## Import some SKOS

To import some vocabulary terms you need to tell the script:

* The user doing the importing
* The vocabulary to import into (which can be created if necessary)

```bash
./scripts/cmd skos-import --createvocabulary --vocabulary ehri-terms --user $USER PATH-TO-SKOS.rdf
```

**NOTE**: The --createvocabulary flag is deprecated and will be replaced/removed in future when an alternative has been implemented.

## Import some EAD

**Note:** The process of importing EAD is currently a bit long-winded due to various database dependencies needing to be in place. In order to import documentary units you first need a "scope" (the repository) to add them to. Repositories likewise need their own scope, which is the country they reside in. (The reason for these dependencies is that the "handle" for any given document is "scoped" by its parents, i.e. the repository and the country item.)

 As a consequence, importing is normally done via scripts that handle this housekeeping automatically, but for this example we'll do it manually:

### Creating a "Country" Item

To create a country item we can use the generic "add" command. The following creates a country identified by the ISO3166 two-letter country-code "gb" (the United Kingdom):

```
./scripts/cmd add country -Pidentifier=gb --user $USER --log "Creating United Kingdom country item."
```

### Creating the "Wiener Library" Repository

Creating a repository would normally be done via importing an EAG (Encoded Archival Guide) file, but for now we will use the "add" command to create a bare-bones entry (with no descriptive data):

```
./scripts/cmd add repository -Pidentifier=wiener-library --user $USER --scope gb --log "Creating repository."
```

(Note the --scope takes the identifier we gave the country as an argument.)

The *scoped identifier* of the repository will be derived from the identifier we provided in a slugified form, plus the slugified identifiers of its parent scope. Therefore we can henceforth refer to this repository as "gb-wiener-library".

To import an EAD file we use the `ead-import` command, refering to the repository scope, like so:

```
export REPO=gb-wiener-library
./scripts/cmd ead-import -user $USER --scope $REPO --log "Importing EAD for repository '$REPO'" --tolerant PATH-TO-EAD-FILE.xml
```


## TODO

Importing EAC/EAG
