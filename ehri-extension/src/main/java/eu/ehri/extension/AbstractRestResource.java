package eu.ehri.extension;

import java.util.List;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.base.Accessor;

public abstract class AbstractRestResource {

    /**
     * With each request the headers of that request are injected into the
     * requestHeaders parameter.
     */
    @Context
    private HttpHeaders requestHeaders;
    /**
     * With each request URI info is injected into the uriInfo parameter.
     */
    @Context
    protected UriInfo uriInfo;
    protected final GraphDatabaseService database;
    protected final FramedGraph<Neo4jGraph> graph;
    protected final GraphManager manager;
    public static final String AUTH_HEADER_NAME = "Authorization";

    public AbstractRestResource(@Context GraphDatabaseService database) {
        this.database = database;
        graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(database));
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Retrieve the id of the UserProfile of the requester
     * 
     * @return The UserProfile
     * @throws BadRequester
     */
    protected Accessor getRequesterUserProfile() throws BadRequester {
        String id = getRequesterIdentifier();
        if (id == null) {
            return AnonymousAccessor.getInstance();
        } else {
            try {
                return getEntity(EntityTypes.USER_PROFILE, id, Accessor.class);
            } catch (ItemNotFound e) {
                throw new BadRequester(id);
            }
        }
    }

    /**
     * Retreive the id string of the requester's UserProfile.
     * 
     * @return
     */
    private String getRequesterIdentifier() {
        List<String> list = requestHeaders.getRequestHeader(AUTH_HEADER_NAME);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Fetch an entity of a given type by its identifier.
     * 
     * @param typeName
     * @param name
     * @param cls
     * @return
     * @throws ItemNotFound
     */
    protected <E> E getEntity(String typeName, String name, Class<E> cls)
            throws ItemNotFound {
        return graph.frame(manager.getVertex(name), cls);
    }
}