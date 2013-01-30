package eu.ehri.project.views.impl;

import java.util.*;
import java.util.Map.Entry;

import org.neo4j.helpers.collection.Iterables;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableSortedMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.util.structures.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.views.Search;
import eu.ehri.project.views.ViewHelper;

/**
 * Handles querying Accessible Entities, with ACL semantics.
 * <p/>
 * TODO: Possibly refactor more of the ACL logic into AclManager.
 *
 * @param <E>
 * @author mike
 */
public final class Query<E extends AccessibleEntity> implements Search<E> {

    public static final int DEFAULT_LIST_LIMIT = 20;

    public static Logger logger = LoggerFactory.getLogger(Query.class);

    private final Optional<Integer> offset;
    private final Optional<Integer> limit;
    private final SortedMap<String, Sort> sort;
    private final Optional<Pair<String, Sort>> defaultSort;
    private final SortedMap<String, Pair<FilterPredicate, String>> filters;
    private final ImmutableMap<Pair<String, Direction>, Integer> depthFilters;
    private final ImmutableMap<QueryUtils.TraversalPath, Pair<FilterPredicate, String>> traversalFilters;
    private final boolean page;

    private final FramedGraph<Neo4jGraph> graph;
    private final GraphManager manager;
    private final Class<E> cls;
    private final ViewHelper helper;
    private final PermissionScope scope;

    /**
     * Directions for sort.
     */
    public static enum Sort {
        ASC, DESC
    }

    ;

    /**
     * Filter predicates
     *
     * @author mike
     */
    public static enum FilterPredicate {
        EQUALS, IEQUALS, STARTSWITH, ENDSWITH, CONTAINS, ICONTAINS, MATCHES, GT, GTE, LT, LTE;
    }

    ;

    /**
     * Full Constructor.
     *
     * @param graph
     * @param cls
     * @param scope
     * @param offset
     * @param limit
     * @param sort
     * @param traversalFilters
     * @param page
     */
    public Query(FramedGraph<Neo4jGraph> graph, Class<E> cls,
            PermissionScope scope, Optional<Integer> offset,
            Optional<Integer> limit, final SortedMap<String, Sort> sort,
            final Optional<Pair<String, Sort>> defSort,
            final SortedMap<String, Pair<FilterPredicate, String>> filters,
            final Map<Pair<String, Direction>, Integer> depthFilters,
            final Map<QueryUtils.TraversalPath, Pair<FilterPredicate, String>> traversalFilters,
            Boolean page) {
        this.graph = graph;
        this.cls = cls;
        this.scope = scope;
        this.offset = offset;
        this.limit = limit;
        this.sort = ImmutableSortedMap.copyOf(sort);
        this.defaultSort = defSort;
        this.filters = ImmutableSortedMap
                .copyOf(filters);
        this.depthFilters = ImmutableMap.copyOf(depthFilters);
        this.traversalFilters = ImmutableMap.copyOf(traversalFilters);
        this.page = page;
        helper = new ViewHelper(graph, scope);
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Simple constructor.
     *
     * @param graph
     * @param cls
     */
    public Query(FramedGraph<Neo4jGraph> graph, Class<E> cls) {
        this(graph, cls, SystemScope.getInstance(),
                Optional.<Integer>absent(), Optional.<Integer>absent(),
                ImmutableSortedMap.<String, Sort>of(), Optional
                .<Pair<String, Sort>>absent(), ImmutableSortedMap
                .<String, Pair<FilterPredicate, String>>of(), Maps
                .<Pair<String, Direction>, Integer>newHashMap(),
                ImmutableSortedMap
                        .<QueryUtils.TraversalPath, Pair<FilterPredicate, String>>of(),
                false);
    }

    /**
     * Scoped Constructor.
     *
     * @param graph
     * @param cls
     * @param scope
     */
    public Query(FramedGraph<Neo4jGraph> graph, Class<E> cls,
            PermissionScope scope) {
        this(graph, cls, scope, Optional.<Integer>absent(), Optional
                .<Integer>absent(), ImmutableSortedMap.<String, Sort>of(),
                Optional.<Pair<String, Sort>>absent(), ImmutableSortedMap
                .<String, Pair<FilterPredicate, String>>of(), Maps
                .<Pair<String, Direction>, Integer>newHashMap(),
                ImmutableSortedMap
                        .<QueryUtils.TraversalPath, Pair<FilterPredicate, String>>of(), false);
    }

    /**
     * Copy constructor.
     *
     * @param other
     */
    public Query<E> copy(Query<E> other) {
        return new Query<E>(other.graph, other.cls, other.scope, other.offset,
                other.limit, other.sort, other.defaultSort, other.filters,
                other.depthFilters, other.traversalFilters, other.page);
    }


    /**
     * Class representing a page of content.
     *
     * @param <T> the item type
     */
    public static class Page<T> {
        private Iterable<T> iterable;
        private long count;
        private Integer offset;
        private Integer limit;
        private Map<String, Sort> sort;

        Page(Iterable<T> iterable, long count, Integer offset, Integer limit,
                Map<String, Sort> sort) {
            this.iterable = iterable;
            this.count = count;
            this.offset = offset;
            this.limit = limit;
            this.sort = sort;
        }

        public Iterable<T> getIterable() {
            return iterable;
        }

        public long getCount() {
            return count;
        }

        public Integer getOffset() {
            return offset;
        }

        public Integer getLimit() {
            return limit;
        }

        public Map<String, Sort> getSort() {
            return sort;
        }
    }

    /**
     * Wrapper method for FramedVertexIterables that converts a
     * FramedVertexIterable<T> back into a plain Iterable<Vertex>.
     *
     * @param <T>
     */
    public static class FramedVertexIterableAdaptor<T extends VertexFrame>
            implements Iterable<Vertex> {
        Iterable<T> iterable;

        public FramedVertexIterableAdaptor(final Iterable<T> iterable) {
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
     * Fetch an item by property id. The first matching item will be returned.
     *
     * @param id
     * @param user
     * @return The matching framed vertex.
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    public E get(String id, Accessor user) throws PermissionDenied,
            ItemNotFound {
        E item = manager.getFrame(id, cls);
        helper.checkReadAccess(item, user);
        return item;
    }

    /**
     * Fetch an item by property key/value. The first matching item will be
     * returned.
     *
     * @param key
     * @param value
     * @param user
     * @return The matching framed vertex.
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    public E get(String key, String value, Accessor user)
            throws PermissionDenied, ItemNotFound {
        CloseableIterable<Vertex> indexQuery = manager.getVertices(key, value,
                ClassUtils.getEntityType(cls));
        try {
            E item = graph.frame(indexQuery.iterator().next(), cls);
            helper.checkReadAccess(item, user);
            return item;
        } catch (NoSuchElementException e) {
            throw new ItemNotFound(key, value);
        } finally {
            indexQuery.close();
        }
    }

    /**
     * Return a Page instance containing a count of total items, and an iterable
     * for the given offset/limit.
     *
     * @param user
     * @return Page instance
     */
    public Page<E> page(Accessor user) {
        return page(ClassUtils.getEntityType(cls), user);
    }

    /**
     * Return a Page instance containing a count of total items, and an iterable
     * for the given offset/limit.
     *
     * @param user
     * @return Page instance
     */
    public Page<E> page(EntityClass type, Accessor user) {
        return page(manager.getFrames(type, cls), user);
    }

    /**
     * Return a Page instance containing a count of total items, and an iterable
     * for the given offset/limit.
     *
     * @param vertices
     * @param user
     * @return Page instance
     */
    public Page<E> page(Iterable<E> vertices, Accessor user) {
        return page(vertices, user, cls);
    }

    /**
     * Return a Page instance containing a count of total items, and an iterable
     * for the given offset/limit.
     *
     * @param vertices
     * @param user
     * @param cls
     * @return Page instance
     */
    public <T extends VertexFrame> Page<T> page(Iterable<T> vertices,
            Accessor user, Class<T> cls) {
        PipeFunction<Vertex, Boolean> aclFilterFunction = new AclManager(graph)
                .getAclFilterFunction(user);

        // FIXME: We have to read the vertices into memory here since we
        // can't re-use the iterator for counting and streaming.
        ArrayList<Vertex> userVerts = Lists.newArrayList(applyFilters(
                new GremlinPipeline<E, Vertex>(
                        new FramedVertexIterableAdaptor<T>(vertices))
                        .filter(aclFilterFunction)).iterator());

        return new Page<T>(graph.frameVertices(
                setPipelineRange(setOrder(applyFilters(new GremlinPipeline<Vertex, Vertex>(
                        userVerts)))), cls), userVerts.size(), offset.or(0),
                limit.or(DEFAULT_LIST_LIMIT), sort);
    }

    /**
     * Return a Page instance containing a count of total items, and an iterable
     * for the given offset/limit.
     *
     * @param key
     * @param query
     * @param user
     * @return Page instance
     */
    public Page<E> page(String key, String query, Accessor user) {
        CloseableIterable<Neo4jVertex> countQ = manager.getVertices(key, query,
                ClassUtils.getEntityType(cls));
        try {
            CloseableIterable<Neo4jVertex> indexQ = manager.getVertices(key,
                    query, ClassUtils.getEntityType(cls));
            try {
                PipeFunction<Vertex, Boolean> aclFilterFunction = new AclManager(
                        graph).getAclFilterFunction(user);
                long count = applyFilters(new GremlinPipeline<Vertex, Vertex>(
                        countQ).filter(aclFilterFunction)).count();
                return new Page<E>(
                        graph.frameVertices(
                                setPipelineRange(setOrder(applyFilters(new GremlinPipeline<Vertex, Vertex>(
                                        indexQ).filter(aclFilterFunction)))),
                                cls), count, offset.or(0),
                        limit.or(DEFAULT_LIST_LIMIT), sort);
            } finally {
                indexQ.close();
            }
        } finally {
            countQ.close();
        }
    }

    /**
     * Apply filtering actions to a Gremlin pipeline.
     *
     * @param pipe
     * @return
     */
    public <S> GremlinPipeline<S, Vertex> applyFilters(GremlinPipeline<S, Vertex> pipe) {
        return setFilters(setDepthFilters(setTraversalFilters(pipe)));
    }

    /**
     * Return an iterable for all items accessible to the user.
     *
     * @param user
     * @return Iterable of framed vertices accessible to the given user
     */
    public Iterable<E> list(Accessor user) {
        return list(ClassUtils.getEntityType(cls), user);
    }

    /**
     * List items accessible to a given user.
     *
     * @param user
     * @return Iterable of items accessible to the given accessor
     */
    public Iterable<E> list(String key, String query, Accessor user) {
        // This function is optimised for ACL actions.
        CloseableIterable<Neo4jVertex> vertices = manager.getVertices(key,
                query, ClassUtils.getEntityType(cls));
        try {
            GremlinPipeline<E, Vertex> filter = new GremlinPipeline<E, Vertex>(
                    vertices).filter(new AclManager(graph)
                    .getAclFilterFunction(user));
            return graph
                    .frameVertices(
                            setPipelineRange(setOrder(applyFilters(filter))),
                            cls);
        } finally {
            vertices.close();
        }
    }

    /**
     * List items accessible to a given user.
     *
     * @param vertices
     * @param user
     * @return Iterable of items accessible to the given accessor
     */
    public Iterable<E> list(Iterable<E> vertices, Accessor user) {
        return list(vertices, user, cls);
    }

    /**
     * List items accessible to a given user.
     *
     * @param vertices
     * @param user
     * @return Iterable of items accessible to the given accessor
     */
    public <T extends VertexFrame> Iterable<T> list(Iterable<T> vertices,
            Accessor user, Class<T> cls) {
        GremlinPipeline<T, Vertex> filter = new GremlinPipeline<T, Vertex>(
                new FramedVertexIterableAdaptor<T>(vertices))
                .filter(new AclManager(graph).getAclFilterFunction(user));
        return graph
                .frameVertices(
                        setPipelineRange(setOrder(applyFilters(filter))),
                        cls);
    }

    /**
     * Return an iterable for all items accessible to the user.
     *
     * @param user
     * @return Iterable of framed vertices accessible to the given user
     */
    public Iterable<E> list(EntityClass type, Accessor user) {
        return list(manager.getFrames(type, cls), user);
    }

    /**
     * Get the offset applied to this query.
     *
     * @return
     */
    public Optional<Integer> getOffset() {
        return offset;
    }

    /**
     * Set the offset applied to this query.
     *
     * @param offset
     */
    public Query<E> setOffset(Integer offset) {
        return new Query<E>(graph, cls, scope, Optional.fromNullable(offset),
                limit, sort, defaultSort, filters, depthFilters, traversalFilters, page);
    }

    /**
     * Get the limit applied to this query.
     *
     * @return
     */
    public Optional<Integer> getLimit() {
        return limit;
    }

    /**
     * Set the limit applied to this query.
     *
     * @param limit
     */
    public Query<E> setLimit(Integer limit) {
        return new Query<E>(graph, cls, scope, offset,
                Optional.fromNullable(limit), sort, defaultSort, filters,
                depthFilters, traversalFilters, page);
    }

    /**
     * Clear the order clause from this query.
     */
    public Query<E> clearOrder() {
        return new Query<E>(graph, cls, scope, offset, limit,
                ImmutableSortedMap.<String, Sort>of(), defaultSort, filters,
                depthFilters, traversalFilters, page);
    }

    /**
     * Add an default order clause, to be used if no custom order is specified.
     *
     * @param field
     * @param order
     */
    public Query<E> defaultOrderBy(String field, Sort order) {

        return new Query<E>(graph, cls, scope, offset, limit, sort,
                Optional.of(new Pair<String, Sort>(field, order)), filters,
                depthFilters, traversalFilters, page);
    }

    /**
     * Add an order clause.
     *
     * @param field
     * @param order
     */
    public Query<E> orderBy(String field, Sort order) {
        SortedMap<String, Sort> tmp = new ImmutableSortedMap.Builder<String, Sort>(
                Ordering.natural()).putAll(sort).put(field, order).build();
        return new Query<E>(graph, cls, scope, offset, limit, tmp, defaultSort,
                filters, depthFilters, traversalFilters, page);
    }

    /**
     * Add a set of string order clauses. Clauses must be of the form:
     * <p/>
     * property__DIRECTION
     *
     * @param orderSpecs list of orderSpecs
     */
    public Query<E> orderBy(Iterable<String> orderSpecs) {
        SortedMap<String, Sort> tmp = parseOrderSpecs(orderSpecs);
        return new Query<E>(graph, cls, scope, offset, limit, tmp, defaultSort,
                filters, depthFilters, traversalFilters, page);
    }

    /**
     * Clear the filter clauses from this query.
     */
    public Query<E> clearFilters() {
        return new Query<E>(
                graph,
                cls,
                scope,
                offset,
                limit,
                sort,
                defaultSort,
                ImmutableSortedMap.<String, Pair<FilterPredicate, String>>of(),
                depthFilters, traversalFilters, page);
    }

    /**
     * Filter out items that are over a certain depth in the given
     * relationship chain.
     *
     * @param label
     * @param depth
     * @return
     */
    public Query<E> depthFilter(String label, Direction direction, Integer depth) {
        Map<Pair<String, Direction>, Integer> tmp = Maps.newHashMap(depthFilters);
        tmp.put(new Pair<String, Direction>(label, direction), depth);
        return new Query<E>(graph, cls, scope, offset, limit, sort,
                defaultSort, filters, tmp, traversalFilters, page);
    }

    /**
     * Add a filter clause.
     *
     * @param property
     * @param predicate predicate
     * @param value
     * @return a new query object
     */
    public Query<E> filter(String property, FilterPredicate predicate,
            String value) {
        SortedMap<String, Pair<FilterPredicate, String>> tmp = new ImmutableSortedMap.Builder<String, Pair<FilterPredicate, String>>(
                Ordering.natural())
                .putAll(filters)
                .put(property,
                        new Pair<FilterPredicate, String>(predicate, value))
                .build();
        return new Query<E>(graph, cls, scope, offset, limit, sort,
                defaultSort, tmp, depthFilters, traversalFilters, page);
    }

    /**
     * Add a filter clause.
     *
     * @param path
     * @param predicate predicate
     * @param value
     * @return a new query object
     */
    public Query<E> filterTraversal(QueryUtils.TraversalPath path, FilterPredicate predicate,
            String value) {
        Map<QueryUtils.TraversalPath, Pair<FilterPredicate, String>> tmp = new ImmutableMap.Builder<QueryUtils.TraversalPath, Pair<FilterPredicate, String>>()
                .putAll(traversalFilters)
                .put(path, new Pair<FilterPredicate, String>(predicate, value))
                .build();
        return new Query<E>(graph, cls, scope, offset, limit, sort,
                defaultSort, filters, depthFilters, tmp, page);
    }

    /**
     * Add a set of unparsed filter clauses. Clauses must be of the format:
     * property__PREDICATE:value
     * <p/>
     * If PREDICATE is omitted, EQUALS is the default.
     *
     * @param filters list of filters
     * @return a new query object
     */
    public Query<E> filter(Iterable<String> filters) {
        // FIXME: This is really gross, but we want to allow the arguments
        // to .filter() to be a mix of property filters and traversal path
        // filters, because they'll usually just come straight from some
        // query params.
        SortedMap<String, Pair<FilterPredicate, String>> tmp = parseFilters(filters);
        Query<E> query = this;
        for (Entry<String, Pair<FilterPredicate, String>> ft : tmp.entrySet()) {
            Optional<QueryUtils.TraversalPath> tpath = QueryUtils.getTraversalPath(ft.getKey());
            if (tpath.isPresent()) {
                logger.debug("Adding traversal filter: {}", tpath.get());
                query = query.filterTraversal(tpath.get(), ft.getValue().getA(), ft.getValue().getB());
            } else {
                logger.debug("Adding property filter: {}", ft.getKey());
                query = query.filter(ft.getKey(), ft.getValue().getA(), ft.getValue().getB());
            }
        }
        return query;
    }

    /**
     * Parse a list of sort specifications.
     *
     * @param orderSpecs
     * @return
     */
    private SortedMap<String, Sort> parseOrderSpecs(Iterable<String> orderSpecs) {
        Builder<String, Sort> builder = new ImmutableSortedMap.Builder<String, Sort>(
                Ordering.natural());
        Splitter psplit = Splitter.on("__");
        for (String spec : orderSpecs) {
            List<String> od = Iterables.toList(psplit.split(spec));
            switch (od.size()) {
                case 1:
                    builder.put(od.get(0), Sort.ASC);
                    break;
                case 2:
                    builder.put(od.get(0), Sort.valueOf(od.get(1)));
                    break;
            }
        }
        return builder.build();
    }

    /**
     * Parse a list of string filter specifications.
     *
     * @param filterList
     * @return
     */
    private SortedMap<String, Pair<FilterPredicate, String>> parseFilters(
            Iterable<String> filterList) {
        Builder<String, Pair<FilterPredicate, String>> builder = new ImmutableSortedMap.Builder<String, Pair<FilterPredicate, String>>(
                Ordering.natural());
        Splitter psplit = Splitter.on("__");
        Splitter vsplit = Splitter.on(":");
        for (String filter : filterList) {
            List<String> kv = Iterables.toList(vsplit.split(filter));
            if (kv.size() == 2) {
                String ppred = kv.get(0);
                String value = kv.get(1);
                List<String> pp = Iterables.toList(psplit.split(ppred));
                switch (pp.size()) {
                    case 1:
                        builder.put(pp.get(0), new Pair<FilterPredicate, String>(
                                FilterPredicate.EQUALS, value));
                        break;
                    case 2:
                        builder.put(pp.get(0), new Pair<FilterPredicate, String>(
                                FilterPredicate.valueOf(pp.get(1)), value));
                        break;
                }

            }
        }
        return builder.build();
    }

    // Helpers

    private <EE> GremlinPipeline<EE, Vertex> setPipelineRange(
            GremlinPipeline<EE, Vertex> filter) {
        int low = Math.max(offset.or(0), 0);
        int high = low + Math.max(limit.or(-1), 0) - 1;
        return filter.range(low, high);
    }

    private <EE> GremlinPipeline<EE, Vertex> setOrder(
            GremlinPipeline<EE, Vertex> pipe) {
        if (sort.isEmpty()) {
            if (defaultSort.isPresent()) {
                return pipe
                        .order(getOrderFunction(new ImmutableSortedMap.Builder<String, Sort>(
                                Ordering.natural()).put(
                                defaultSort.get().getA(),
                                defaultSort.get().getB()).build()));
            }
            return pipe;
        }
        return pipe.order(getOrderFunction(sort));
    }

    private <EE> GremlinPipeline<EE, Vertex> setFilters(
            GremlinPipeline<EE, Vertex> pipe) {
        if (filters.isEmpty())
            return pipe;
        return pipe.filter(getFilterFunction());
    }

    private <EE> GremlinPipeline<EE, Vertex> setTraversalFilters(
            GremlinPipeline<EE, Vertex> pipe) {
        if (traversalFilters.isEmpty())
            return pipe;
        return pipe.filter(getTraversalFilterFunction());
    }

    private <EE> GremlinPipeline<EE, Vertex> setDepthFilters(
            GremlinPipeline<EE, Vertex> pipe) {
        if (depthFilters.isEmpty())
            return pipe;
        return pipe.filter(getDepthFilterFunction());
    }

    private PipeFunction<Pair<Vertex, Vertex>, Integer> getOrderFunction(
            final SortedMap<String, Sort> sort) {
        return new PipeFunction<Pair<Vertex, Vertex>, Integer>() {
            public Integer compute(Pair<Vertex, Vertex> pair) {
                ComparisonChain chain = ComparisonChain.start();
                for (Entry<String, Sort> entry : sort.entrySet()) {
                    if (entry.getValue().equals(Sort.ASC)) {
                        chain = chain.compare(
                                (String) pair.getA()
                                        .getProperty(entry.getKey()),
                                (String) pair.getB()
                                        .getProperty(entry.getKey()));
                    } else {
                        chain = chain.compare(
                                (String) pair.getB()
                                        .getProperty(entry.getKey()),
                                (String) pair.getA()
                                        .getProperty(entry.getKey()));
                    }
                }
                return chain.result();
            }
        };
    }

    /**
     * Create a filter that limits the selected nodes to with a particular depth
     * of a relationship chain. For example, if a set of nodes form a
     * hierachical relationship such as:
     * <p/>
     * child -[childOf]-> parent -[childOf]-> grandparent
     * <p/>
     * Then a depthFilter of childOf -> 0 would filter out all except the
     * grandparent node.
     * <p/>
     * TODO: Figure out how to do this will Gremlin's loop() construct.
     *
     * @return
     */
    private PipeFunction<Vertex, Boolean> getDepthFilterFunction() {
        return new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex vertex) {
                for (Entry<Pair<String, Direction>, Integer> entry : depthFilters.entrySet()) {
                    int depthCount = 0;
                    Vertex tmp = vertex;
                    String label = entry.getKey().getA();
                    Direction direction = entry.getKey().getB();
                    while (tmp.getEdges(direction, label)
                            .iterator().hasNext()) {
                        tmp = tmp.getVertices(direction, label)
                                .iterator().next();
                        depthCount++;
                        if (depthCount > entry.getValue()) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };
    }

    private PipeFunction<Vertex, Boolean> getFilterFunction() {
        return new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex vertex) {
                for (Entry<String, Pair<FilterPredicate, String>> entry : filters
                        .entrySet()) {
                    String p = (String) vertex.getProperty(entry.getKey());
                    if (p == null || !matches(p, entry.getValue().getA(), entry.getValue()
                            .getB())) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    private PipeFunction<Vertex, Boolean> getTraversalFilterFunction() {
        // FIXME: Make this less horribly inefficient!
        return new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex vertex) {
                for (Entry<QueryUtils.TraversalPath, Pair<FilterPredicate, String>> entry : traversalFilters
                        .entrySet()) {
                    Vertex current = vertex;
                    GremlinPipeline<Vertex, Vertex> pipeline = getFilterTraversalPipeline(
                            vertex, entry.getKey(), entry.getValue());
                    if (!pipeline.hasNext()) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    private GremlinPipeline<Vertex, Vertex> getFilterTraversalPipeline(
            final Vertex startVertex, final QueryUtils.TraversalPath traversalPath,
            final Pair<FilterPredicate, String> filter) {
        GremlinPipeline<Vertex, Vertex> p = new GremlinPipeline<Vertex, Vertex>(startVertex);
        for (Pair<String, Direction> tp : traversalPath.getTraversals()) {
            switch (tp.getB()) {
                case IN:
                    logger.debug("Adding in relation: {}", tp.getA());
                    p = p.in(tp.getA());
                    break;
                case OUT:
                    logger.debug("Adding out relation: {}", tp.getA());
                    p = p.out(tp.getA());
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected direction in traversal pipe function: " + tp.getB());
            }
        }
        return p.filter(new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex vertex) {
                String p = (String) vertex.getProperty(traversalPath.getProperty());
                return p != null && matches(p, filter.getA(), filter.getB());
            }
        });
    }


    // FIXME: This has several limitations so far.
    // - only handles properties cast as strings
    // - doesn't do case-insensitive regexp matching.
    private boolean matches(String a, FilterPredicate predicate, String b) {
        switch (predicate) {
            case EQUALS:
                return a.equals(b);
            case IEQUALS:
                return a.equalsIgnoreCase(b);
            case STARTSWITH:
                return a.startsWith(b);
            case ENDSWITH:
                return a.endsWith(b);
            case CONTAINS:
                return a.contains(b);
            case ICONTAINS:
                return a.toLowerCase().contains(b.toLowerCase());
            case MATCHES:
                return a.matches(b);
            case GT:
                return a.compareTo(b) > 0;
            case GTE:
                return a.compareTo(b) >= 0;
            case LT:
                return a.compareTo(b) < 0;
            case LTE:
                return a.compareTo(b) <= 0;
            default:
                throw new RuntimeException("Unexpected filter predicate: "
                        + predicate);
        }
    }
}
