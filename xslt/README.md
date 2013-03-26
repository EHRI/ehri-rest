Solr Indexing
=============

Strategy for indexing EHRI graph data into Solr.
--------------
Solr has an API for updating the index with XML data and the EHRI neo4j extension has an API for extracting the data as XML. 
Therfore it was a natural choice to transform the neo4j output to Solr input using XSLT. 
The transformation is then specified by an XSL file and we can provide different fikles for different transforms.   

Updating (or constructing) the Solr index from the command line is done in the following steps.

1. Use the ehri commandline tool to extract XML representation of data in the graph database.
Note that you need a 'graph.db' data directory that is not locked by a running neo4j server.
Therefore it is a sort of 'offline' procedure and not updating the data from a 'live' system.
There was an option implemented using the database  in a 'read-only' mode 
that would work on a 'live' system, but it didn't work on OSX so we temporary dropped it. 

2. Convert the extracted XML into XML that can be sent to the Solr service.
We will do this with xslt, which then needs an xsl file that specifies the conversion.

3. The resulting Solr XML can be imported into Solr by using the REST API provided by Solr. 

The whole procedure could be scripted of course.

Instead of running the export, transform and update from the command line and have intermediate files it could be implemented in a Java application or service. 
 

The indexing fields
------------------- 
The entity descriptions are in one language and an entity has one description per language. Instead of indexing the entities and 'flattening' all its descriptions in a single Solr doc, the entity descriptions are being indexed. 
For each description the entity id and type of the entity being described is indexed. 
In this way the entity can be retrieved from the graph using the description search result.  

Except for the required unique id (key) all fields are using socalled dynamic fields like  ``*_s`` and ``*_txt`` and nothing EHRI specific. 
The fields for the description 'properties' get their name based on the property name. 
All this makes the XSL more generic and minimizes the need for specifying fields in the solr configuration (schema.xml). 

Besides indexing in property specific fields all properties are also indexed in a 'collective' text field. This allows you to search for 'all' text and not only for a specific field. 
Language specific searching is provided in a similar way, but then with a text field that is language specific. The Solr schema handles those fiels differently and in a language specific way by accounting for stemming, stopwords and synonyms. 
Note that indexing the same data in several fields can also be accompliced by 'CopyField' entries in the the Solr schema. 


Below an example of the output of transforming a documentaryUnit with XSLT: 

    <add>
       <doc>
          <field name="id">cd2</field>
          <field name="documentId_s">c2</field>
          <field name="type_s">documentaryUnit</field>
          <field name="lang_s">en</field>
          <field name="holderId_s">r1</field>
          <field name="accessibleTo_ss">admin</field>
          <field name="accessibleTo_ss">tim</field>
          <field name="title_txt">Documentary Unit 2</field>
          <field name="txt">Documentary Unit 2</field>
          <field name="txt_en">Documentary Unit 2</field>
          <field name="identifier_txt">c2-desc</field>
          <field name="txt">c2-desc</field>
          <field name="txt_en">c2-desc</field>
       </doc>
    </add>


Searching on documentaryUnits with an english description:

A specific property

    title_txt:'somevalue' AND type_s:'documentaryUnit' AND lang_s:'en'

Any property

    text:'somevalue' AND type_s:'documentaryUnit' AND lang_s:'en'

Searching for a 'match' using the english language is different:

    text_en:'somevalue' AND type_s:'documentaryUnit'
 


Permissions with Solr
---------------------
Solr queries that respect the EHRI permissions. 

The query results need to be filtered in order to remove results that the requesting user is not allowed to see. 
  Filtering after getting all the search results is not workable 
  because it doesn't scale. 

The solution is to use the search engine to do the filtering. 
  We store the id's of the users and groups that have permission on an indexed document 
  inside a special multi-valued string field: ``accessibleTo_ss``.
 Whenever you query you can restrict the results by specifying that the ``accessibleTo_ss`` 
 contains the id of the requesting user or one of it's groups. 
 When user 'paul' of group 'admin' does a request the query looks like this: 

     (<original query>) AND (accessibleTo_ss:'paul' OR accessibleTo_ss:'admin')

 More groups could be added by OR'ing. 
 One problem remains, and that is how we handle the case that there is no restriction. 
 We could introduce a special user id ``_all_`` or something similar, but then we would have to make sure nobody gets that id. 
 The current solution is that we just have no id's, so a non existing ``accessibleTo_ss`` field means 
 that everyone is allowed. However this complicates the query considerably, and it might make it slower.  
 
 To 'detect' that a Solr field has not been set you need to use ``NOT fieldname:[* TO *]``, 
 but we must AND it with 'everything' because the NOT works like an exclusion. 
 We know that the 'id' is a required field, so we need to AND with that: 

    (<original query>) AND ((id:[* TO *] AND NOT accessibleTo_ss:[* TO *]) OR accessibleTo_txt:"paul" OR accessibleTo_txt:"admin")

