package eu.ehri.plugin;

import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Name;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.ObjectToRepresentationConverter;
import org.neo4j.server.rest.repr.Representation;

import eu.ehri.data.EhriNeo4j;

/*
 * The rest interface for neo4j needs to be extended with utilities that do some extra work for us.
 * Those could be accompliced by several calls to the rest api, 
 * but need to be atomic and therefore we like them to be new rest calls. 
 * 
 * Note that it is by no means intended as a 'public' REST interface, but only for out Ehri system. 
 * 
 * Instead of Node and Relationship we use Vertex and Edge in the method naming. 
 */

@Description("Plugin with utilities, which will be used to persists data for the EHRI collection registry")
public class EhriNeo4jPlugin extends ServerPlugin {

	/**
	 * 
	 * @param graphDB
	 * @param index
	 * @param field
	 * @param query
	 * @return
	 * @throws Exception
	 */
	@Name("simpleQuery")
	@Description("Fetch a collection with the given name.")
	@PluginTarget(GraphDatabaseService.class)
	public Representation simpleQuery(
			@Source GraphDatabaseService graphDB,
			@Description("Index to query")
			@Parameter(name = "index", optional = false) String index,
			@Description("Field to query on")
			@Parameter(name = "field", optional = false) String field,
			@Description("The query string")
			@Parameter(name = "query", optional = false) String query
		) throws Exception {
		return ObjectToRepresentationConverter.convert(
			EhriNeo4j.simpleQuery(graphDB, index, field, query)
		);
	}
	
	/*** Vertex ***/

	/**
	 * Example with curl:
	 * curl -X POST -H "Content-type: application/json" http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/createIndexedVertex -d '{"index":"test", "data": {"name": "Test1"}}'
	 * 
	 * @param graphDB
	 * @param data
	 * @param index
	 * @return
	 * @throws Exception
	 */
	@Name("createIndexedVertex")
	@Description("Create an indexed vertex")
	@PluginTarget(GraphDatabaseService.class)
	public Representation createIndexedVertex(
			@Source GraphDatabaseService graphDB,
			@Description("Data")
			@Parameter(name = "data", optional = false) Map data,
			@Description("Index name")
			@Parameter(name = "index", optional = false) String index
		) throws Exception {
		return ObjectToRepresentationConverter.convert(
			EhriNeo4j.createIndexedVertex(graphDB, data, index)
		);
	}
	
	/**
	 * Example with curl:
	 * curl -X POST -H "Content-type: application/json" http://localhost:7474/db/data/ext/EhriNeo4jPlugin/graphdb/deleteVertex -d '{"id":"80449"}'
	 * 
	 * @param graphDB
	 * @param id
	 * @return
	 * @throws Exception
	 */
	@Name("deleteVertex")
	@Description("Delete a vertex")
	@PluginTarget(GraphDatabaseService.class)
	public Representation deleteVertex(
			@Source GraphDatabaseService graphDB,
			@Description("Vertex identifier")
			@Parameter(name = "id", optional = false) long id
		) throws Exception {
		EhriNeo4j.deleteVertex(graphDB, id);
		// TODO other results on failure
		return ObjectToRepresentationConverter.convert(
			"deleted"
		);
	}
	
	/**
	 * 
	 * @param graphDB
	 * @param id
	 * @param data
	 * @param index
	 * @return
	 * @throws Exception
	 */
	@Name("updateIndexedVertex")
	@Description("Update an indexed vertex")
	@PluginTarget(GraphDatabaseService.class)
	public Representation updateIndexedVertex(
			@Source GraphDatabaseService graphDB,
			@Description("Vertex identifier")
			@Parameter(name = "id", optional = false) long id,
			@Description("Data")
			@Parameter(name = "data", optional = false) Map data,
			@Description("Index name")
			@Parameter(name = "index", optional = false) String index
		) throws Exception {
		return ObjectToRepresentationConverter.convert(
			EhriNeo4j.updateIndexedVertex(graphDB, id, data, index)
		);
	}

	/*** Edge ***/
	
	/**
	 * 
	 * @param graphDB
	 * @param outV
	 * @param typeLabel
	 * @param inV
	 * @param data
	 * @param index
	 * @return
	 * @throws Exception
	 */
	@Name("createIndexedEdge")
	@Description("Create an indexed edge")
	@PluginTarget(GraphDatabaseService.class)
	public Representation createIndexedEdge(
			@Source GraphDatabaseService graphDB,
			@Description("Outgoing vertex id")
			@Parameter(name = "outV", optional = false) long outV, 
			@Description("Edge type")
			@Parameter(name = "typeLabel", optional = false) String typeLabel, 
			@Description("Ingoing vertex id")
			@Parameter(name = "inV", optional = false) long inV,
			@Description("Data")
			@Parameter(name = "data", optional = false) Map data,
			@Description("Index name")
			@Parameter(name = "index", optional = false) String index
		) throws Exception {
		return ObjectToRepresentationConverter.convert(
			EhriNeo4j.createIndexedEdge(graphDB, outV, typeLabel, inV, data, index)
		);
	}

	/**
	 * 
	 * @param graphDB
	 * @param id
	 * @return
	 * @throws Exception
	 */
	@Name("deleteEdge")
	@Description("Delete an edge")
	@PluginTarget(GraphDatabaseService.class)
	public Representation deleteEdge(
			@Source GraphDatabaseService graphDB,
			@Description("Edge identifier")
			@Parameter(name = "id", optional = false) long id
		) throws Exception {
		EhriNeo4j.deleteEdge(graphDB, id);
		// TODO other results on failure
		return ObjectToRepresentationConverter.convert(
			"deleted"
		);
	}
	
	/**
	 * 
	 * @param graphDB
	 * @param id
	 * @param data
	 * @param index
	 * @return
	 * @throws Exception
	 */
	@Name("updateIndexedEdge")
	@Description("update an indexed edge")
	@PluginTarget(GraphDatabaseService.class)
	public Representation updateIndexedEdge(
			@Source GraphDatabaseService graphDB,
			@Description("Edge identifier")
			@Parameter(name = "id", optional = false) long id,
			@Description("Data")
			@Parameter(name = "data", optional = false) Map data,
			@Description("Index name")
			@Parameter(name = "index", optional = false) String index
		) throws Exception {
		return ObjectToRepresentationConverter.convert(
			EhriNeo4j.updateIndexedEdge(graphDB, id, data, index)
		);
	}
}