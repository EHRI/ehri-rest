package eu.ehri.project.utils;

import java.util.HashMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.ActionManager;

/**
 * Initialize the graph with a minimal set of vertices. This includes:
 * <p/>
 * - an admin account - permissions - content types
 *
 * @author michaelb
 */
public class GraphInitializer {
    private final FramedGraph<? extends TransactionalGraph> graph;
    private final GraphManager manager;

    private static final String INIT_MESSAGE = "Initialising graph";

    public GraphInitializer(FramedGraph<? extends TransactionalGraph> graph) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
    }

    @SuppressWarnings("serial")
    public void initialize() {
        try {

            // Create the system node which is the head of the global event streams
            manager.createVertex(ActionManager.GLOBAL_EVENT_ROOT, EntityClass.SYSTEM,
                    ImmutableMap.<String, Object>of(
                            // It might be useful to know when this graph was
                            // initialise. We can also put other metadata here.
                            SystemEvent.TIMESTAMP, ActionManager.getTimestamp(),
                            SystemEvent.LOG_MESSAGE, INIT_MESSAGE
                    ));

            // Create admin account
            manager.createVertex(Group.ADMIN_GROUP_IDENTIFIER,
                    EntityClass.GROUP, new HashMap<String, Object>() {
                {
                    put(Group.IDENTIFIER_KEY, Group.ADMIN_GROUP_IDENTIFIER);
                    put(Group.NAME, Group.ADMIN_GROUP_NAME);
                }
            });

            // Create permission nodes corresponding to the Enum values
            for (final PermissionType pt : PermissionType.values()) {
                manager.createVertex(pt.getName(), EntityClass.PERMISSION,
                        Maps.<String, Object>newHashMap());
            }

            // Create content type nodes corresponding to the Enum values
            for (final ContentTypes ct : ContentTypes.values()) {
                manager.createVertex(ct.getName(), EntityClass.CONTENT_TYPE,
                        Maps.<String, Object>newHashMap());
            }
            graph.getBaseGraph().commit();
        } catch (Exception e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        }
    }
}
