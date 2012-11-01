package eu.ehri.extension;

import java.util.List;
import java.util.NoSuchElementException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.base.AccessibleEntity;

public class AbstractRestResource {

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
    public static final String AUTH_HEADER_NAME = "Authorization";

    public AbstractRestResource(@Context GraphDatabaseService database) {
        this.database = database;
        graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(database));        
    }

    /**
     * Retrieve the id of the UserProfile of the requester
     * 
     * @return The vertex id
     * @throws PermissionDenied
     */
    protected Long getRequesterUserProfileId() throws PermissionDenied {
        String id = getRequesterIdentifier();
        if (id == null) {
            return null;
        } else {
            // just take the first one and get the Long value
            Index<Vertex> index = graph.getBaseGraph().getIndex(
                    EntityTypes.USER_PROFILE, Vertex.class);
            CloseableIterable<Vertex> query = index.get(
                    AccessibleEntity.IDENTIFIER_KEY, id);
            try {
                return (Long) query.iterator().next().getId();
            } catch (NoSuchElementException e) {
                return null;
            } finally {
                query.close();
            }
        }
    }
    
    protected String getRequesterIdentifier() {
        List<String> list = requestHeaders.getRequestHeader(AUTH_HEADER_NAME);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null; 
    }

}