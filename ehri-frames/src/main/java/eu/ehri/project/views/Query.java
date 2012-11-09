package eu.ehri.project.views;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedVertexIterable;
import com.tinkerpop.frames.VertexFrame;
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
    private Integer offset = null;
    private Integer limit = null;
    private String sort = null;
    private boolean page = false;

    /**
     * Wrapper method for FramedVertexIterables that converts a FramedVertexIterable<T>
     * back into a plain Iterable<Vertex>.
     * @author michaelb
     *
     * @param <T>
     */
    public static class FramedVertexIterableAdaptor<T extends VertexFrame> implements Iterable<Vertex> {
        FramedVertexIterable<T> iterable;
        public FramedVertexIterableAdaptor(final FramedVertexIterable<T> iterable) {
            this.iterable = iterable;
        }
        public Iterator<Vertex> iterator() {
            return new Iterator<Vertex>() {
                private Iterator<T> iterator = iterable.iterator();
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                public boolean hasNext() {
                    return this.iterator.hasNext(); 
                }
                public Vertex next() {
                    return this.iterator.next().asVertex();
                }
            };
        }
    }
    
    /**
     * Constructor.
     * 
     * @param graph
     * @param cls
     */
    public Query(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        super(graph, cls);
    }

    /**
     * Copy constructor.
     * 
     * @param other
     */
    public Query(Query<E> other) {
        super(other.graph, other.cls);
        this.scope = other.scope;
        this.offset = other.offset;
        this.limit = other.limit;
        this.sort = other.sort;
        this.page = other.page;
    }

    public E get(String key, String value, Accessor user)
            throws PermissionDenied, ItemNotFound {
        try {
            CloseableIterable<Vertex> indexQuery = getIndexForClass(cls).get(
                    key, value);
            try {
                E item = graph.frame(indexQuery.iterator().next(), cls);
                checkReadAccess(item, user);
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
    public Iterable<E> list(Accessor user) {
        return list(AccessibleEntity.IDENTIFIER_KEY, QUERY_GLOB, user);
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
    public Iterable<E> list(String key, String query, Accessor user) {
        // This function is optimised for ACL actions.
        try {
            CloseableIterable<Vertex> indexQuery = getIndexForClass(cls).query(
                    key, query);
            try {
                GremlinPipeline filter = new GremlinPipeline(indexQuery)
                        .filter(new AclManager(graph)
                                .getAclFilterFunction(user));
                return graph.frameVertices(
                        setPipelineRange(filter), cls);
            } finally {
                indexQuery.close();
            }
        } catch (IndexNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    private GremlinPipeline setPipelineRange(GremlinPipeline filter) {
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

    public int getOffset() {
        return offset;
    }

    public Query<E> setOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public Query<E> setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public String getSort() {
        return sort;
    }

    public Query<E> setSort(String sort) {
        this.sort = sort;
        return this;
    }
}
