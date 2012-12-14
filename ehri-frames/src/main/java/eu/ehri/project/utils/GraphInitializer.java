package eu.ehri.project.utils;

import java.util.HashMap;

import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
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

/**
 * Initialize the graph with a minimal set of vertices. This
 * includes:
 * 
 *  - an admin account
 *  - permissions
 *  - content types
 *  
 * @author michaelb
 *
 */
public class GraphInitializer {
    private final FramedGraph<Neo4jGraph> graph;
    private final GraphManager manager;
    
    public GraphInitializer(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
        manager = GraphManagerFactory.getInstance(graph);
    }
    
    @SuppressWarnings("serial")
    public void initialize() {
        try {
            
            // Create admin account
            manager.createVertex(Group.ADMIN_GROUP_IDENTIFIER, EntityClass.GROUP, new HashMap<String, Object>() {{
                put(Group.IDENTIFIER_KEY, Group.ADMIN_GROUP_IDENTIFIER);
            }});
            
            // Create permission nodes corresponding to the Enum values
            for (final PermissionType pt : PermissionType.values()) {
                manager.createVertex(pt.getName(), EntityClass.PERMISSION, new HashMap<String, Object>() {{
                    put(Permission.IDENTIFIER_KEY, pt.getName());
                }});                
            }

            // Create content type nodes corresponding to the Enum values
            for (final ContentTypes ct : ContentTypes.values()) {
                manager.createVertex(ct.getName(), EntityClass.CONTENT_TYPE, new HashMap<String, Object>() {{
                    put(ContentType.IDENTIFIER_KEY, ct.getName());
                }});                
            }            
            graph.getBaseGraph().stopTransaction(Conclusion.SUCCESS);
        } catch (Exception e) {
            graph.getBaseGraph().stopTransaction(Conclusion.FAILURE);
            throw new RuntimeException(e);
        }
    }
}
