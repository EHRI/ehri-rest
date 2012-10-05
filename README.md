Neo4j server plugin for use by the EHRI project
===============================================
The 'neo4j-ehri-plugin' provides an internal RESTfull API 
for the collection registry of the EHRI project WP19. 

This server plugin is actually implemented by two extensions: 
the managed extension or plugin and the unmanaged extension simply called extension. 
The 'ehri-plugin' and 'ehri-extension' projects both depend on the 'ehri-frames' project, 
which handles the business logic and data persistency. 

Building and deploying the plugin:
----------------------------------
  
  Two directories that are important are the NEO4J home directory {neo4j-home} where you installed neo4j 
  and the plugin maven root directory {neo4j-ehri-plugin} where you downloaded the project code. 

1. build and copy jars
    > cd {neo4j-ehri-plugin}
    > mvn clean install
    > cp ehri-plugin/target/ehri-plugin-0.1-SNAPSHOT.jar {neo4j-home}/plugins   
    > cp ehri-extension/target/ehri-extension-0.0.1-SNAPSHOT.jar {neo4j-home}/plugins   

    The plugin depends on ehri-frames; 
    therefore you need to copy the 'ehri-frames-0.1-SNAPSHOT.jar' into the 
    '{neo4j-home}/system/lib'
    Any jars that the ehri-data-frames uses also need to be placed into the 
    '{neo4j-home}/system/lib'
    For the current version that means:
        - blueprints-core-2.1.0.jar
        - blueprints-neo4j-graph-2.1.0.jar
        - frames-2.1.0.jar
        - pipes-2.1.0.jar
        - joda-time-2.1.jar

2. restart neo4j
    > cd {neo4j-home}/bin
    > ./neo4j restart

3. check plugin status
    > curl localhost:7474/db/data/
should mention the EhriNeo4jPlugin


**NOTE:** 
The code is now dependent on e special version of neo4j, the existing version uses a blueprints that is not compatible with the blueprints we use in our own code. 
This blueprints version problem will be fixed by the neo4j development team, but we have to wait for it!


System tests for the 'neo4j-ehri-plugin'
----------------------------------------
This system test section shows you how to use curl to try out the REST interface.  
This is must useful for developers when testing changes to the system. 

Although the neo4j-ehri-plugin java projects have integration (unit) tests 
it is best to be able to test if API also works when deployed on a (test) server. 
When errors are only occurring in a deployed state, the neo4j server can be remotely debugged using the Eclipse IDE. 
This is explained on the neo4j site: http://docs.neo4j.org/chunked/1.4.M04/server-debugging.html

Below it is explained how to use the curl command for testing the system. 
The testing of the whole system can also be focused more towards acceptance testing 
or performance testing. For that kind of testing other tools could be more useful, like Pythons scripts or JMeter for instance. 


Note for making JSON testdata: 
it is helpful to use an online editor like http://www.jsoneditoronline.org/  
  
### The managed extension or plugin
These are all POST requests with json data.
The more low-level interface allows you to manage (CRUD) vertices, edges and indices 
without authentication or passing of any user id. 

Create an indexed Vertex
> curl -v -X POST -H "Content-type: application/json"
>   http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/createIndexedVertex
>   -d '{"index":"test", "data": {"name": "Test1"}}'
     
Delete it     
> curl -v -X POST -H "Content-type: application/json"
>   http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/deleteVertex -d'{"id":"80449"}'

The next example is a bit more complex and creates a user 
that can be used as an admin user in the next section for the unmanaged extension. 
Note that you can't use the unmanaged extension for that if you haven't got an 'admin' user allready. 

1. Create an admin group vertex
> curl -v -X POST -H "Content-type: application/json" 
>   http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/getOrCreateVertexIndex"
>   -d'{"index":"group", "parameters": {}}'
> curl -v -X POST -H "Content-type: application/json" 
>   http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/createIndexedVertex"
>   -d'{"index":"group", "data": {"name": "admin", "isA": "group"}}'

2. Create an admin userProfile vertex
> curl -v -X POST -H "Content-type: application/json" 
>   http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/getOrCreateVertexIndex"
>   -d'{"index":"userProfile", "parameters": {}}'
> curl -v -X POST -H "Content-type: application/json" 
>   http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/createIndexedVertex"
>   -d'{"index":"userProfile", "data": {"userId": 0, "name": "admin", "isA": "userProfile"}}'

3. Create a belongsTo edge from user to group
if group is 5678 and user is 1234.
> curl -v -X POST -H "Content-type: application/json" 
>   http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/getOrCreateEdgeIndex"
>   -d'{"index":"belongsTo", "parameters": {}}'
> curl -v -X POST -H "Content-type: application/json" 
>   http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/createIndexedEdge"
>   -d'{"index":"belongsTo", "outV": 1234, "inV": 5678, "typeLabel": "belongsTo" , "data": {}}'


### The unmanaged extension
This is more RESTful then the managed extension, 
because we use not only POST, but also GET, PUT and DELETE.
This interface allows you to manage (CRUD) on EHRI specific data from the ehri-frames, 
like DocumentaryUnits, Agents and UserProfile's.  
Also no authentication, but you need to provide a valid user id with every call. 

Assume that we have an admin user with id 80497. 

> curl -v -X POST -H "Authorization: 80497" -H "Accept: application/json" -H "Content-type: application/json"
> http://localhost:7474/ehri/documentaryUnit -d
>   '{"data":{"name":"a collection","identifier":"some id",
>   "isA":"documentaryUnit"
>   },"relationships":{"describes":[{"data":{"identifier":"some id",
>   "title":"a description"
>   ,"isA":"documentDescription","languageOfDescription":"en"}}],
>   "hasDate":[{"data":{"startDate":"1940-01-01T00:00:00Z","endDate":
>   "1945-01-01T00:00:00Z", "isA":"datePeriod"}}]}}'

And it's id is 80501.

> curl -v -X GET -H "Authorization: 80497" -H "Accept: application/json"
>   http://localhost:7474/ehri/documentaryUnit/80501

> curl -v -X DELETE -H "Authorization: 80497" -H "Accept: application/json"
>   http://localhost:7474/ehri/documentaryUnit/80501
