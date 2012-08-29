Neo4j server plugin for use by the EHRI project
===============================================

  Building and deploying the plugin:
  
  Two directories that are important are the NEO4J home directory <neo4j-home> where you installed neo4j 
  and the plugin maven root directory <neo4j-ehri-plugin> where you downloaded the project code.
- build and copy jars
  # cd <neo4j-ehri-plugin>
  # mvn clean install
  # cp ehri-data/target/ehri-data-0.1-SNAPSHOT.jar <neo4j-home>/system/lib   
  # cp ehri-plugin/target/ehri-plugin-0.1-SNAPSHOT.jar <neo4j-home>/plugins   
- restart neo4j
  # cd <neo4j-home>/bin
  # ./neo4j restart
- check plugin status
  # curl localhost:7474/db/data/
  should mention the EhriNeo4jPlugin
