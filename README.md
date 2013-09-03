# Neo4j server plugin for use by the EHRI project

The 'neo4j-ehri-plugin' provides an internal RESTful API
for the collection registry of the EHRI project WP19. 

This server plugin is actually implemented by an unmanaged neo4j extension. 
This 'ehri-extension' project depends mainly on the 'ehri-frames' project, 
which handles the business logic and data persistency. 

##Building and deploying the plugin:
----------------------------------
  
### Prerequisites 
* Java6
* Maven3
* Git (if you want to contribute or get the latest versions from GitHub)
* Most likely a Java IDE
* Neo4j server
    The directory where neo4j is installed will be called {neo4j} from now on. 
      
For documentation (a work-in-progress, but better than nothing) see the docs:

* [Installing and running from the code repository](docs/INSTALL.md)
* [Importing data](docs/IMPORT.md)
* [Writing management scripts](docs/SCRIPTING.md)