package eu.ehri.plugin;

import java.util.Map;

import eu.ehri.data.EhriNeo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.*;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.ObjectToRepresentationConverter;

/*
 * The rest interface for neo4j needs to be extended with utilities that do some extra work for us.
 * Those could be accompliced by several calls to the rest api, 
 * but need to be atomic and therefore we like them to be new rest calls. 
 * 
 * Instead of Node and Relationship we use Vertex and Edge in the method naming. 
 */

@Description("Plugin with utilities, which will be used to persists data for the EHRI collection registry")
public class EhriNeo4jPlugin extends ServerPlugin {

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