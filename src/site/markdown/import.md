# Importing via the Command Line

First, initialise a new Neo4j database as described in the [initialise instructions](initialise.md). Since we'll
be using the command line here, don't re-start the Neo4j server instance.

**Note**: these docs cover importing data using an in-process Neo4j instance. For EAC/EAD/EAG import the
[Web Service](web-service.md) docs provide a more convenient method of ingesting data.

## Import some SKOS

To import some vocabulary terms you first need to create a vocabulary to which they will belong. We do this with the
generic "add" command:

```bash
./scripts/cmd add CvocVocabulary -Pidentifier=ehri-terms --user $USER --log "Creating vocabulary for ehri terms."
```

The `id` handle of the created vocabulary will be derived from the "identifier" field. Since there is no enclosing
scope, the resulting id will just be `ehri-terms`. We reference this for the scope parameter for the SKOS import
command:

```bash
./scripts/cmd skos-import --scope ehri-terms --user $USER --tolerant --user $USER  --log "Importing EHRI SKOS" PATH-TO-SKOS.rdf
```

## Import some EAD

**Note:** The process of importing EAD is currently a bit long-winded due to various database dependencies needing to be in place. In order to import documentary units you first need a "scope" (the repository) to add them to. Repositories likewise need their own scope, which is the country they reside in. (The reason for these dependencies is that the "handle" for any given document is "scoped" by its parents, i.e. the repository and the country item.)

 As a consequence, importing is normally done via scripts that handle this housekeeping automatically, but for this example we'll do it manually:

### Creating a "Country" Item

To create a country item we can use the generic "add" command. The following creates a country identified by the ISO3166 two-letter country-code "gb" (the United Kingdom):

```
./scripts/cmd add Country -Pidentifier=gb --user $USER --log "Creating United Kingdom country item."
```

### Creating the "Wiener Library" Repository

Creating a repository would normally be done via importing an EAG (Encoded Archival Guide) file, but for now we will use the "add" command to create a bare-bones entry (with no descriptive data):

```
./scripts/cmd add Repository -Pidentifier=wiener-library --user $USER --scope gb --log "Creating repository."
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

