# Neo4j server plugin for use by the EHRI project

The 'neo4j-ehri-plugin' provides an internal RESTful API
for the collection registry of the EHRI project WP19. 

This server plugin is actually implemented by an unmanaged neo4j extension. 
This 'ehri-extension' project depends mainly on the 'ehri-frames' project, 
which handles the business logic and data persistency. 

For documentation (a work-in-progress, but better than nothing) see the docs:

* [Installing and running from the code repository](docs/INSTALL.md)
* [Importing data](docs/IMPORT.md)
* [Writing management scripts](docs/SCRIPTING.md)