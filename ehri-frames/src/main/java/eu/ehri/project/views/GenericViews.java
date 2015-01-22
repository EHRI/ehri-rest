package eu.ehri.project.views;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.base.Accessor;

import java.util.List;

/**
 * Views for fetching any type of {@link eu.ehri.project.acl.ContentTypes}
 * via string or internal graph IDs, handling filtering for ACL.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class GenericViews {

    private final FramedGraph<?> graph;
    private final AclManager acl;
    private final GraphManager manager;

    /**
     * Constructor.
     *
     * @param graph the framed graph
     */
    public GenericViews(FramedGraph<?> graph) {
        this.graph = graph;
        acl = new AclManager(graph);
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Get an item by string id.
     *
     * @param id       the item's id
     * @param accessor the current accessor
     * @return the item's vertex
     * @throws ItemNotFound
     * @throws AccessDenied
     */
    public Vertex get(String id, Accessor accessor) throws ItemNotFound, AccessDenied {
        Vertex vertex = manager.getVertex(id);
        return filterItem(vertex, accessor);
    }

    /**
     * Get an item by internal graph id.
     *
     * @param gid      the item's graph id
     * @param accessor the current accessor
     * @return the item's vertex
     * @throws ItemNotFound
     * @throws AccessDenied
     */
    public Vertex getByGid(Object gid, Accessor accessor) throws ItemNotFound, AccessDenied {
        Vertex vertex = graph.getVertex(gid);
        if (vertex == null) throw new ItemNotFound(gid.toString());
        return filterItem(vertex, accessor);
    }

    /**
     * Get a list of item vertices by internal graph id.
     *
     * @param gids     a list of graph ids
     * @param accessor the current accessor
     * @param strict   determines the behaviour if an item is not found
     *                 in the graph. If true, a missing item will throw
     *                 an {@link eu.ehri.project.exceptions.ItemNotFound}
     *                 exception. If false, the item will be omitted from
     *                 the output iterable.
     * @return a lazy iterable of item vertices
     * @throws ItemNotFound
     */
    public Iterable<Vertex> listByGid(List<Object> gids, Accessor accessor, boolean strict) throws ItemNotFound {
        Iterable<Vertex> vertices = strict
                ? getVerticesByGidStrict(gids)
                : getVerticesByGid(gids);
        return filterList(vertices, accessor);
    }

    /**
     * Get a list of item vertices by string id.
     *
     * @param ids      a list of string ids
     * @param accessor the current accessor
     * @param strict   determines the behaviour if an item is not found
     *                 in the graph. If true, a missing item will throw
     *                 an {@link eu.ehri.project.exceptions.ItemNotFound}
     *                 exception. If false, the item will be omitted from
     *                 the output iterable.
     * @return a lazy iterable of item vertices
     * @throws ItemNotFound
     */
    public Iterable<Vertex> list(List<String> ids, Accessor accessor, boolean strict) throws ItemNotFound {
        Iterable<Vertex> vertices = strict
                ? getVerticesStrict(ids)
                : getVertices(ids);
        return filterList(vertices, accessor);
    }

    private Iterable<Vertex> filterList(Iterable<Vertex> items, Accessor accessor) {
        PipeFunction<Vertex, Boolean> filter = acl
                .getAclFilterFunction(accessor);
        return new GremlinPipeline<Vertex, Vertex>(items)
                .filter(acl.getContentTypeFilterFunction()).filter(filter);
    }

    private Vertex filterItem(Vertex vertex, Accessor accessor) throws ItemNotFound, AccessDenied {
        // If the item doesn't exist or isn't a content type throw 404
        if (!acl.getContentTypeFilterFunction().compute(vertex)) {
            throw new ItemNotFound(vertex.getId().toString());
        } else if (!acl.getAclFilterFunction(accessor).compute(vertex)) {
            throw new AccessDenied(accessor.getId(), vertex.getId().toString());
        }
        return vertex;
    }

    private Iterable<Vertex> getVerticesStrict(List<String> ids) throws ItemNotFound {
        return manager.getVertices(ids);
    }

    private Iterable<Vertex> getVerticesByGidStrict(List<Object> gids) throws ItemNotFound {
        for (Object gid : gids) {
            if (graph.getVertex(gid) == null) {
                throw new ItemNotFound(gid.toString());
            }
        }

        return FluentIterable.from(gids)
                .transform(new Function<Object, Vertex>() {
                    @Override
                    public Vertex apply(Object gid) {
                        return graph.getVertex(gid);
                    }
                });
    }

    private Iterable<Vertex> getVerticesByGid(List<Object> gids) {
        return FluentIterable.from(gids)
                .transform(new Function<Object, Vertex>() {
                    @Override
                    public Vertex apply(Object gid) {
                        return graph.getVertex(gid);
                    }
                }).filter(new Predicate<Vertex>() {
                    @Override
                    public boolean apply(Vertex vertex) {
                        return vertex != null;
                    }
                });
    }

    private Iterable<Vertex> getVertices(List<String> ids) {
        return FluentIterable.from(ids)
                .transform(new Function<String, Vertex>() {
                    @Override
                    public Vertex apply(String s) {
                        try {
                            return manager.getVertex(s);
                        } catch (ItemNotFound e) {
                            return null;
                        }
                    }
                }).filter(new Predicate<Vertex>() {
                    @Override
                    public boolean apply(Vertex vertex) {
                        return vertex != null;
                    }
                });
    }
}
