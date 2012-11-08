package eu.ehri.project.views;

import java.util.NoSuchElementException;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.exceptions.IndexNotFoundException;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;

/**
 * Handles querying Accessible Entities, with ACL semantics.
 * 
 * TODO: Possibly refactor more of the ACL logic into AclManager.
 * 
 * @author mike
 * 
 * @param <E>
 */
public class Query<E extends AccessibleEntity> extends AbstractViews<E>
        implements IQuery<E> {
    private static final String QUERY_GLOB = "*";

    /**
     * Constructor.
     * 
     * @param graph
     * @param cls
     */
    public Query(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        super(graph, cls);
    }

    public E get(String key, String value, Accessor user) throws PermissionDenied,
            ItemNotFound {
        try {
            CloseableIterable<Vertex> indexQuery = getIndexForClass(cls).get(
                    key, value);
            try {
                E item = graph.frame(indexQuery.iterator().next(), cls);
                checkReadAccess(item, getAccessor(user));
                return item;
            } catch (NoSuchElementException e) {
                throw new ItemNotFound(key, value);
            } finally {
                indexQuery.close();
            }
        } catch (IndexNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return an iterable for all items.
     * 
     * @param user
     * @return
     * @throws IndexNotFoundException
     */
    public Iterable<E> list(Integer offset, Integer limit, Accessor user) {
        return list(AccessibleEntity.IDENTIFIER_KEY, QUERY_GLOB, offset, limit,
                user);
    }

    /**
     * List items accessible to a given user.
     * 
     * @param user
     * 
     * @return
     * @throws IndexNotFoundException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Iterable<E> list(String key, String query, Integer offset,
            Integer limit, Accessor user) {

        // This function is optimised for ACL actions.
        try {
            CloseableIterable<Vertex> indexQuery = getIndexForClass(cls).query(
                    key, query);
            try {
                GremlinPipeline filter = new GremlinPipeline(indexQuery)
                        .filter(new AclManager(graph)
                                .getAclFilterFunction(user));
                return graph.frameVertices(
                        setPipelineRange(filter, offset, limit), cls);
            } finally {
                indexQuery.close();
            }
        } catch (IndexNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    private GremlinPipeline setPipelineRange(GremlinPipeline filter,
            Integer offset, Integer limit) {
        int low = offset == null ? 0 : Math.max(offset, 0);
        int high = limit == null ? -1 : low + Math.max(limit, 0) - 1;
        return filter.range(low, high);
    }

    private Index<Vertex> getIndexForClass(Class<E> cls)
            throws IndexNotFoundException {
        Index<Vertex> index = graph.getBaseGraph().getIndex(
                getEntityIndexName(cls), Vertex.class);
        if (index == null)
            throw new IndexNotFoundException(getEntityIndexName(cls));
        return index;
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
