package eu.ehri.plugin;

import java.util.ArrayList;
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

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.core.GraphHelpers;

/*
 * The rest interface for neo4j needs to be extended with utilities that do some extra work for us.
 * Those could be accompliced by several calls to the rest API, 
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
            @Source GraphDatabaseService graphDb,
            @Description("Index to query") @Parameter(name = "index", optional = false) String index,
            @Description("Field to query on") @Parameter(name = "field", optional = false) String field,
            @Description("The query string") @Parameter(name = "query", optional = false) String query)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        return ObjectToRepresentationConverter.convert(helpers.simpleQuery(
                index, field, query));
    }

    /*** Vertex ***/

    /**
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
            @Source GraphDatabaseService graphDb,
            @Description("Data") @Parameter(name = "data", optional = false) Map data,
            @Description("Index name") @Parameter(name = "index", optional = false) String index)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        return ObjectToRepresentationConverter.convert(helpers
                .createIndexedVertex(data, index));
    }

    /**
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
            @Source GraphDatabaseService graphDb,
            @Description("Vertex identifier") @Parameter(name = "id", optional = false) long id)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        helpers.deleteVertex(id);
        // TODO other results on failure
        return ObjectToRepresentationConverter.convert("deleted");
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
            @Source GraphDatabaseService graphDb,
            @Description("Vertex identifier") @Parameter(name = "id", optional = false) long id,
            @Description("Data") @Parameter(name = "data", optional = false) Map data,
            @Description("Index name") @Parameter(name = "index", optional = false) String index)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        return ObjectToRepresentationConverter.convert(helpers
                .updateIndexedVertex(id, data, index));
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
            @Source GraphDatabaseService graphDb,
            @Description("Outgoing vertex id") @Parameter(name = "outV", optional = false) long outV,
            @Description("Edge type") @Parameter(name = "typeLabel", optional = false) String typeLabel,
            @Description("Ingoing vertex id") @Parameter(name = "inV", optional = false) long inV,
            @Description("Data") @Parameter(name = "data", optional = false) Map data,
            @Description("Index name") @Parameter(name = "index", optional = false) String index)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        return ObjectToRepresentationConverter.convert(helpers
                .createIndexedEdge(outV, inV, typeLabel, data, index));
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
            @Source GraphDatabaseService graphDb,
            @Description("Edge identifier") @Parameter(name = "id", optional = false) long id)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        helpers.deleteEdge(id);
        // TODO other results on failure
        return ObjectToRepresentationConverter.convert("deleted");
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
            @Source GraphDatabaseService graphDb,
            @Description("Edge identifier") @Parameter(name = "id", optional = false) long id,
            @Description("Data") @Parameter(name = "data", optional = false) Map data,
            @Description("Index name") @Parameter(name = "index", optional = false) String index)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        return ObjectToRepresentationConverter.convert(helpers
                .updateIndexedEdge(id, data, index));
    }

    /*** Index ***/

    @Name("getVertexIndex")
    @Description("get the index")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getVertexIndex(
            @Source GraphDatabaseService graphDb,
            @Description("Index name") @Parameter(name = "index", optional = false) String index)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        return ObjectToRepresentationConverter.convert(helpers.getIndex(index,
                Vertex.class));
    }

    @Name("getOrCreateVertexIndex")
    @Description("get the index or create it if it did not exist")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getOrCreateVertexIndex(
            @Source GraphDatabaseService graphDb,
            @Description("Index name") @Parameter(name = "index", optional = false) String index,
            @Description("Parameters") @Parameter(name = "parameters", optional = false) Map parameters)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        return ObjectToRepresentationConverter.convert(helpers
                .getOrCreateIndex(index, Vertex.class,
                        getParametersAsArray(parameters)));
    }

    @Name("getEdgeIndex")
    @Description("get the index")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getEdgeIndex(
            @Source GraphDatabaseService graphDb,
            @Description("Index name") @Parameter(name = "index", optional = false) String index)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        return ObjectToRepresentationConverter.convert(helpers.getIndex(index,
                Edge.class));
    }

    @Name("getOrCreateEdgeIndex")
    @Description("get the index or create it if it did not exist")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getOrCreateEdgeIndex(
            @Source GraphDatabaseService graphDb,
            @Description("Index name") @Parameter(name = "index", optional = false) String index,
            @Description("Parameters") @Parameter(name = "parameters", optional = false) Map parameters)
            throws Exception {
        GraphHelpers helpers = new GraphHelpers(graphDb);
        return ObjectToRepresentationConverter.convert(helpers
                .getOrCreateIndex(index, Edge.class,
                        getParametersAsArray(parameters)));
    }

    // convert the parameters map to an array of Parameters
    // construct List from parameter Map
    // and then have the list in place of the varargs
    // Note that the server plugin has an abstract ParemeterList class
    private com.tinkerpop.blueprints.Parameter[] getParametersAsArray(
            Map<String, Object> parameters) {
        ArrayList<com.tinkerpop.blueprints.Parameter> parametersList = new ArrayList<com.tinkerpop.blueprints.Parameter>();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getValue() == null)
                continue;
            parametersList
                    .add(new com.tinkerpop.blueprints.Parameter<String, Object>(
                            entry.getKey(), entry.getValue()));
        }
        return (com.tinkerpop.blueprints.Parameter[]) parametersList
                .toArray(new com.tinkerpop.blueprints.Parameter[parametersList
                        .size()]);
    }

}