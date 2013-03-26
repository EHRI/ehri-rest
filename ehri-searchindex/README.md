ehri-searchindex
================
This webservice provides a RESTfull interface 
for indexing the data from the ehri Neo4j graph db into Solr. 
It needs to have Neo4j and Solr running. 

# Configuration

 configuration of the service

  The url's of those services must be specified in the configuration file. 
  Also the directory wher the stylesheets can be found must be specified here. 
  
	> 	/ehri-searchindex/src/main/resources/config.properties

location of files to use

- stylesheets for converting entities of a certain type to the Solr index document

	> 	/neo4j-ehri-plugin/xslt/*_solr.xsl

- solr schema used for indexing

	> 	/neo4j-ehri-plugin/xslt/schema.xml


# Examples
The following curl examples assume that the service is deployed on localhost port 8080 

- index the entity with id="paul"

	> 	curl -v -X GET http://localhost:8080/ehri-searchindex/rest/indexer/index/paul

- delete the entity with id="paul"

	> 	curl -v -X DELETE http://localhost:8080/ehri-searchindex/rest/indexer/index/paul

- index all the entity of type userProfile

	> 	curl -v -X GET http://localhost:8080/ehri-searchindex/rest/indexer/index/type/userProfile

