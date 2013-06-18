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

You can delete the index of that type with curl on the Solr directly

	>	curl http://localhost:8080/solr-ehri/registry/update --data-binary '<delete><query>type_s:userProfile</query></delete>' -H 'Content-type:application/xml'

	>	curl http://localhost:8080/solr-ehri/registry/update --data-binary '<commit/>' -H 'Content-type:application/xml'
 
Where the exact url depend on how you have Solr deployed.  


# Solr installation notes
The current version of the ehri-searchindex has been tested 
with a specific Solr setup described below. 

- A multicore setup of solr4.1 (http://lucene.apache.org/solr/) with a 'registry' core for the neo4j data

- Need to place the schema.xml in the ehri/registry/conf. 
  
This file can be found in the project /neo4j-ehri-plugin/xslt/schema.xml
To be able to index langiuage specific you need tio have the following in it: 

  	<field name="text_pl" type="text_pl" indexed="true" stored="true" multiValued="true"/>
	<field name="text_bg" type="text_bg" indexed="true" stored="true" multiValued="true"/>
	<field name="text_ca" type="text_ca" indexed="true" stored="true" multiValued="true"/>
	<field name="text_cz" type="text_cz" indexed="true" stored="true" multiValued="true"/>
	<field name="text_da" type="text_da" indexed="true" stored="true" multiValued="true"/>
	<field name="text_de" type="text_de" indexed="true" stored="true" multiValued="true"/>
	<field name="text_el" type="text_el" indexed="true" stored="true" multiValued="true"/>
	<field name="text_es" type="text_es" indexed="true" stored="true" multiValued="true"/>
	<field name="text_eu" type="text_eu" indexed="true" stored="true" multiValued="true"/>
	<field name="text_fi" type="text_fi" indexed="true" stored="true" multiValued="true"/>
	<field name="text_fr" type="text_fr" indexed="true" stored="true" multiValued="true"/>
	<field name="text_ga" type="text_ga" indexed="true" stored="true" multiValued="true"/>
	<field name="text_gl" type="text_gl" indexed="true" stored="true" multiValued="true"/>
	<field name="text_hu" type="text_hu" indexed="true" stored="true" multiValued="true"/>
	<field name="text_it" type="text_it" indexed="true" stored="true" multiValued="true"/>
	<field name="text_lv" type="text_lv" indexed="true" stored="true" multiValued="true"/>
	<field name="text_nl" type="text_nl" indexed="true" stored="true" multiValued="true"/>
	<field name="text_no" type="text_no" indexed="true" stored="true" multiValued="true"/>
	<field name="text_pt" type="text_pt" indexed="true" stored="true" multiValued="true"/>
	<field name="text_ro" type="text_ro" indexed="true" stored="true" multiValued="true"/>
	<field name="text_ru" type="text_ru" indexed="true" stored="true" multiValued="true"/>
	<field name="text_sv" type="text_sv" indexed="true" stored="true" multiValued="true"/>

  
- Extended with the polish analyser. 

For Polish (text_pl) use the Morfologik filter. 

The *.jar files are in the distribution. The relevant files are:

  apache-solr-analysis-extras-4.1.0.jar  (/solr-4.1.0/dist)
  lucene-analyzers-morfologik-4.1.0.jar (/solr-4.1.0/contrib/analysis-extras/lucene-libs)
  morfologik-fsa-1.5.5.jar (/solr-4.1.0/contrib/analysis-extras/lib/)
  morfologik-polish-1.5.5.jar (/solr-4.1.0/contrib/analysis-extras/lib/) and
  morfologik-stemming-1.5.5.jar (/solr-4.1.0/contrib/analysis-extras/lib/)

Copy those to the ehri/registry/lib directory. 
A stopwords_pl.txt file with the Polish stopwords list from Wikipedia needs to be placed in 
ehri/registry/conf/lang. 

The ehri/registry/conf/schema.xml file needs to have these lines:


      <!-- Polish -->
      <fieldType name="text_pl" class="solr.TextField" positionIncrementGap="100">
      <analyzer> 
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
        <filter class="solr.StopFilterFactory" ignoreCase="true" words="lang/stopwords_pl.txt" enablePositionIncrements="true"/>
    <filter class="solr.MorfologikFilterFactory" dictionary="MORFOLOGIK" />
      </analyzer>
    </fieldType>
     
