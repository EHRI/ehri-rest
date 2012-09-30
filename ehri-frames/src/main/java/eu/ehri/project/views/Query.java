package eu.ehri.project.views;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Converter;

/**
 * Handles querying Accessible Entities, with ACL semantics.
 * 
 * TODO: Possibly refactor more of the ACL logic into AclManager.
 * 
 * @author mike
 * 
 * @param <E>
 */
public class Query<E extends AccessibleEntity> implements IQuery<E> {
    private static final String QUERY_GLOB = "*";
    protected final FramedGraph<Neo4jGraph> graph;
    protected final Class<E> cls;
    protected final Converter converter = new Converter();
    protected final AclManager acl;

    /**
     * Constructor.
     * 
     * @param graph
     * @param cls
     */
    public Query(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this.graph = graph;
        this.cls = cls;
        this.acl = new AclManager(graph);
    }

    /**
     * Return an iterable for all items.
     * 
     * @param user
     * @return
     */
    public Iterable<E> list(long user) {
        return list(AccessibleEntity.IDENTIFIER_KEY, QUERY_GLOB, user);
    }

    /**
     * List items accessible to a given user.
     * 
     * @param user
     * 
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Iterable<E> list(String key, String query, long user) {

        // This function is optimised for ACL actions.
        Accessor accessor = graph.getVertex(user, Accessor.class);
        Index<Vertex> index = graph.getBaseGraph().getIndex(
                getEntityIndexName(cls), Vertex.class);
        CloseableIterable<Vertex> indexQuery = index.query(key, query);

        try {
            GremlinPipeline filter = new GremlinPipeline(indexQuery)
                    .filter(new AclManager(graph)
                            .getAclFilterFunction(accessor));
            return graph.frameVertices(filter, cls);
        } finally {
            indexQuery.close();
        }
    }

    /**
     * Get the Entity index type key...
     * 
     * @param cls
     * @return
     */
    private String getEntityIndexName(Class<E> cls) {
        EntityType ann = cls.getAnnotation(EntityType.class);
        if (ann != null)
            return ann.value();
        return null;
    }
}
