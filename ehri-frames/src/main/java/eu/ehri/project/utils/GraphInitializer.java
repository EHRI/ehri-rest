package eu.ehri.project.utils;

import java.util.HashMap;

import com.google.common.collect.ImmutableMap;
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.ActionManager;

/**
 * Initialize the graph with a minimal set of vertices. This includes:
 * 
 * - an admin account - permissions - content types
 * 
 * @author michaelb
 * 
 */
public class GraphInitializer {
    private final FramedGraph<Neo4jGraph> graph;
    private final GraphManager manager;

    private static final String INIT_MESSAGE = "Initialising graph";

    public GraphInitializer(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
    }

    @SuppressWarnings("serial")
    public void initialize() {
        try {

            // Create the system node which is the head of the global event streams
            manager.createVertex(ActionManager.GLOBAL_EVENT_ROOT, EntityClass.SYSTEM,
                    ImmutableMap.<String,Object>of(
                        // It might be useful to know when this graph was
                        // initialise. We can also put other metadata here.
                        eu.ehri.project.models.events.SystemEvent.TIMESTAMP, ActionManager.getTimestamp(),
                        SystemEvent.LOG_MESSAGE, INIT_MESSAGE
                    ));

            // Create admin account
            manager.createVertex(Group.ADMIN_GROUP_IDENTIFIER,
                    EntityClass.GROUP, new HashMap<String, Object>() {
                        {
                            put(Group.IDENTIFIER_KEY,
                                    Group.ADMIN_GROUP_IDENTIFIER);
                            put(Group.NAME, "Administrators");
                        }
                    });

            // Create permission nodes corresponding to the Enum values
            for (final PermissionType pt : PermissionType.values()) {
                manager.createVertex(pt.getName(), EntityClass.PERMISSION,
                        new HashMap<String, Object>() {
                            {
                                put(Permission.IDENTIFIER_KEY, pt.getName());
                            }
                        });
            }

            // Create content type nodes corresponding to the Enum values
            for (final ContentTypes ct : ContentTypes.values()) {
                manager.createVertex(ct.getName(), EntityClass.CONTENT_TYPE,
                        new HashMap<String, Object>() {
                            {
                                put(ContentType.IDENTIFIER_KEY, ct.getName());
                            }
                        });
            }
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        } catch (Exception e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("serial")
    public void createAdminUserProfile(final String id, final String name) {
        try {
            Vertex user = manager.createVertex(id, EntityClass.USER_PROFILE,
                    new HashMap<String, Object>() {
                        {
                            put(UserProfile.IDENTIFIER_KEY, id);
                            put(UserProfile.NAME, name);
                        }
                    });
            manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class)
                    .addMember(graph.frame(user, UserProfile.class));
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        } catch (Exception e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }
}
