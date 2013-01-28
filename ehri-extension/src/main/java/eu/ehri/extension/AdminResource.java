package eu.ehri.extension;

// Borrowed, temporarily, from Michael Hunger:
// https://github.com/jexp/neo4j-clean-remote-db-addon

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

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.Serializer;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.impl.LoggingCrudViews;

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
    private Serializer serializer;
    private GraphManager manager;

    public AdminResource(@Context Database database) {
        this.database = database;
        this.graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(
                database.getGraph()));
        serializer = new Serializer(graph);
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
            Bundle bundle = new Bundle(EntityClass.USER_PROFILE)
                    .withDataValue(AccessibleEntity.IDENTIFIER_KEY, ident)
                    .withDataValue(Accessor.NAME, ident);

            // NB: This assumes that admin's ID is the same as its identifier.
            Accessor accessor = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER,
                    Accessor.class);
            Crud<UserProfile> view = new LoggingCrudViews<UserProfile>(graph,
                    UserProfile.class);
            UserProfile user = view.create(bundle, accessor);
            // Grant them owner permissions on their own account.
            new AclManager(graph).grantPermissions(user, user,
                    PermissionType.OWNER);

            String jsonStr = serializer.vertexFrameToJson(user);
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
        CloseableIterable<Vertex> query = manager
                .getVertices(EntityClass.USER_PROFILE);
        try {
            for (@SuppressWarnings("unused")
            Vertex _ : query)
                userCount++;
        } finally {
            query.close();
        }
        long start = userCount + 1;
        while (manager.exists(String.format(DEFAULT_USER_ID_FORMAT,
                DEFAULT_USER_ID_PREFIX, start)))
            start++;
        return String.format(DEFAULT_USER_ID_FORMAT, DEFAULT_USER_ID_PREFIX,
                start);
    }
}
