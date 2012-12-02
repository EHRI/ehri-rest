package eu.ehri.extension;

// Borrowed, temporarily, from Michael Hunger:
// https://github.com/jexp/neo4j-clean-remote-db-addon

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.Transaction;
import org.neo4j.server.database.Database;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.persistance.Converter;

/**
 * Provides a RESTfull interface for the Action class. Note: Action instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 */
@Path("admin")
public class AdminResource {

    public static String DEFAULT_USER_ID_PREFIX = "user";
    public static String DEFAULT_USER_ID_FORMAT = "%s%06d";

    private Database database;
    private FramedGraph<Neo4jGraph> graph;
    private Converter converter;
    private GraphManager manager;

    public AdminResource(@Context Database database) {
        this.database = database;
        this.graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(
                database.getGraph()));
        converter = new Converter();
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Create a new user with a default name and identifier.
     * 
     * @return
     * @throws Exception
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/createDefaultUserProfile")
    public Response createDefaultUserProfile() throws Exception {
        Transaction tx = database.getGraph().beginTx();
        try {
            String ident = getNextDefaultUserId();
            Map<String, Object> data = new HashMap<String, Object>();
            data.put(AccessibleEntity.IDENTIFIER_KEY, ident);
            data.put(Accessor.NAME, ident);

            // TODO: Create an action for this with the system user...
            BundleDAO<UserProfile> persister = new BundleDAO<UserProfile>(graph);
            UserProfile user = persister
                    .create(new BundleFactory<UserProfile>().buildBundle(data,
                            UserProfile.class));
            String jsonStr = converter.vertexFrameToJson(user);
            tx.success();
            return Response.status(Status.CREATED).entity((jsonStr).getBytes())
                    .build();
        } catch (Exception e) {
            tx.failure();
            throw e;
        } finally {
            tx.finish();
        }
    }

    // Helpers...

    private String getNextDefaultUserId() {
        // FIXME: It's crappy to have to iterate all the items to count them...
        long userCount = 0;
        CloseableIterable<Vertex> query = manager.getVertices(EntityTypes.USER_PROFILE);
        try {
            for (@SuppressWarnings("unused")
            Vertex _ : query)
                userCount++;
        } finally {
            query.close();
        }
        long start = userCount + 1;
        while (manager.exists(String.format(
                DEFAULT_USER_ID_FORMAT, DEFAULT_USER_ID_PREFIX, start)))
            start++;
        return String.format(DEFAULT_USER_ID_FORMAT, DEFAULT_USER_ID_PREFIX,
                start);
    }
}
