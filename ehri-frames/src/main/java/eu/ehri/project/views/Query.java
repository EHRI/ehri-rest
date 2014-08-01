    package eu.ehri.project.views;

    import com.google.common.base.Optional;
    import com.google.common.collect.*;
    import com.tinkerpop.blueprints.CloseableIterable;
    import com.tinkerpop.blueprints.Direction;
    import com.tinkerpop.blueprints.Vertex;
    import com.tinkerpop.frames.FramedGraph;
    import com.tinkerpop.gremlin.java.GremlinPipeline;
    import com.tinkerpop.pipes.PipeFunction;
    import com.tinkerpop.pipes.util.structures.Pair;
    import eu.ehri.project.acl.AclManager;
    import eu.ehri.project.acl.SystemScope;
    import eu.ehri.project.core.GraphManager;
    import eu.ehri.project.core.GraphManagerFactory;
    import eu.ehri.project.exceptions.AccessDenied;
    import eu.ehri.project.exceptions.ItemNotFound;
    import eu.ehri.project.exceptions.PermissionDenied;
    import eu.ehri.project.models.EntityClass;
    import eu.ehri.project.models.base.AccessibleEntity;
    import eu.ehri.project.models.base.Accessor;
    import eu.ehri.project.models.base.Frame;
    import eu.ehri.project.models.base.PermissionScope;
    import eu.ehri.project.models.utils.ClassUtils;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.util.*;
    import java.util.Map.Entry;

/**
 * Handles querying Accessible Entities, with ACL semantics.
 * <p/>
 * TODO: Possibly refactor more of the ACL logic into AclManager.
 *
 * @param <E>
 * @author mike
 */
public final class Query<E extends AccessibleEntity> {

    public static final int DEFAULT_LIST_LIMIT = 20;

    private static final Logger logger = LoggerFactory.getLogger(Query.class);

    private final Optional<Integer> offset;
    private final Optional<Integer> limit;
    private final SortedMap<String, Sort> sort;
    private final SortedMap<QueryUtils.TraversalPath, Sort> traversalSort;
    private final Optional<Pair<String, Sort>> defaultSort;
    private final SortedMap<String, Pair<FilterPredicate, String>> filters;
    private final ImmutableMap<Pair<String, Direction>, Integer> depthFilters;
    private final List<GremlinPipeline<Vertex, Vertex>> traversalFilters;
    private final boolean page;

    private final FramedGraph<?> graph;
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

    /**
     * Filter predicates
     *
     * @author mike
     */
    public static enum FilterPredicate {
        EQUALS, IEQUALS, STARTSWITH, ENDSWITH, CONTAINS, ICONTAINS, MATCHES, GT, GTE, LT, LTE
    }

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
    private Query(FramedGraph<?> graph, Class<E> cls,
            PermissionScope scope,
            final Optional<Integer> offset,
            final Optional<Integer> limit,
            final SortedMap<String, Sort> sort,
            final SortedMap<QueryUtils.TraversalPath, Sort> traversalSort,
            final Optional<Pair<String, Sort>> defSort,
            final SortedMap<String, Pair<FilterPredicate, String>> filters,
            final Map<Pair<String, Direction>, Integer> depthFilters,
            final List<GremlinPipeline<Vertex, Vertex>> traversalFilters,
            final Boolean page) {
        this.graph = graph;
        this.cls = cls;
        this.scope = scope;
        this.offset = offset;
        this.limit = limit;
        this.sort = ImmutableSortedMap.copyOf(sort);
        this.traversalSort = ImmutableSortedMap.copyOf(traversalSort);
        this.defaultSort = defSort;
        this.filters = ImmutableSortedMap
                .copyOf(filters);
        this.depthFilters = ImmutableMap.copyOf(depthFilters);
        this.traversalFilters = ImmutableList.copyOf(traversalFilters);
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
    public Query(FramedGraph<?> graph, Class<E> cls) {
        this(graph, cls, SystemScope.getInstance(),
                Optional.<Integer>absent(), Optional.<Integer>absent(),
                ImmutableSortedMap.<String, Sort>of(), ImmutableSortedMap
                .<QueryUtils.TraversalPath, Sort>of(), Optional
                .<Pair<String, Sort>>absent(), ImmutableSortedMap
                .<String, Pair<FilterPredicate, String>>of(), Maps
                .<Pair<String, Direction>, Integer>newHashMap(),
                ImmutableList.<GremlinPipeline<Vertex, Vertex>>of(),
                false);
    }

    /**
     * Copy constructor.
     *
     * @param other
     */
    public Query<E> copy(Query<E> other) {
        return new Query<E>(other.graph, other.cls, other.scope, other.offset,
                other.limit, other.sort, other.traversalSort, other.defaultSort, other.filters,
                other.depthFilters, other.traversalFilters, other.page);
    }


    /**
     * Class representing a page of content.
     *
     * @param <T> the item type
     */
    public static class Page<T> {
        private final Iterable<T> iterable;
        private final long count;
        private final Integer offset;
        private final Integer limit;
        private final Map<String, Sort> sort;

        public Page(Iterable<T> iterable, long count, Integer offset, Integer limit,
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
    public static class FramedVertexIterableAdaptor<T extends Frame>
            implements Iterable<Vertex> {
        final Iterable<T> iterable;

        public FramedVertexIterableAdaptor(final Iterable<T> iterable) {
            this.iterable = iterable;
        }

        public Iterator<Vertex> iterator() {
            return new Iterator<Vertex>() {
                private final Iterator<T> iterator = iterable.iterator();

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
    public <T extends Frame> Page<T> page(Iterable<T> vertices,
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
        CloseableIterable<Vertex> countQ = manager.getVertices(key, query,
                ClassUtils.getEntityType(cls));
        try {
            CloseableIterable<Vertex> indexQ = manager.getVertices(key,
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
    private <S> GremlinPipeline<S, Vertex> applyFilters(GremlinPipeline<S, Vertex> pipe) {
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
     * Count items accessible to a given user.
     * @param user
     * @return
     */
    public Long count(Accessor user) {
        return count(ClassUtils.getEntityType(cls), user);
    }

    /**
     * List items accessible to a given user.
     *
     * @param user
     * @return Iterable of items accessible to the given accessor
     */
    public Iterable<E> list(String key, String query, Accessor user) {
        // This function is optimised for ACL actions.
        CloseableIterable<Vertex> vertices = manager.getVertices(key,
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
    public <T extends Frame> Iterable<T> list(Iterable<T> vertices,
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
     * Count items accessible to a given user.
     *
     * @param vertices
     * @param user
     * @return Long count of items accessible to the given accessor
     */
    public <T> Long count(Iterable<T> vertices, Accessor user) {
        GremlinPipeline<T, Vertex> filter = new GremlinPipeline<T, Vertex>(vertices);

        // FIXME! Count doesn't 'account' for ACL privileges!
                //.filter(new AclManager(graph).getAclFilterFunction(user));
        return applyFilters(filter).count();
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
     * Return an iterable for all items accessible to the user.
     *
     * @param user
     * @return Iterable of framed vertices accessible to the given user
     */
    public Long count(EntityClass type, Accessor user) {
        return count(manager.getVertices(type), user);
    }

    /**
     * Set the offset applied to this query.
     *
     * @param offset
     */
    public Query<E> setOffset(Integer offset) {
        return new Query<E>(graph, cls, scope, Optional.fromNullable(offset),
                limit, sort, traversalSort, defaultSort, filters, depthFilters, traversalFilters, page);
    }

    /**
     * Set the limit applied to this query.
     *
     * @param limit
     */
    public Query<E> setLimit(Integer limit) {
        return new Query<E>(graph, cls, scope, offset,
                Optional.fromNullable(limit), sort, traversalSort, defaultSort, filters,
                depthFilters, traversalFilters, page);
    }

    /**
     * Add an default order clause, to be used if no custom order is specified.
     *
     * @param field
     * @param order
     */
    public Query<E> defaultOrderBy(String field, Sort order) {

        return new Query<E>(graph, cls, scope, offset, limit, sort, traversalSort,
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
        return new Query<E>(graph, cls, scope, offset, limit, tmp, traversalSort, defaultSort,
                filters, depthFilters, traversalFilters, page);
    }

    public Query<E> orderByTraversal(QueryUtils.TraversalPath tp, Sort order) {
        ImmutableSortedMap.Builder<QueryUtils.TraversalPath, Sort> tmp = new ImmutableSortedMap.Builder<QueryUtils.TraversalPath, Sort>(
                Ordering.arbitrary()).putAll(traversalSort);
        tmp.put(tp, order);
        return new Query<E>(graph, cls, scope, offset, limit, sort, tmp.build(), defaultSort,
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
        SortedMap<String, Sort> tmp = QueryUtils.parseOrderSpecs(orderSpecs);
        Query<E> query = this;
        for (Entry<String, Sort> entry : tmp.entrySet()) {
            Optional<QueryUtils.TraversalPath> tp = QueryUtils.getTraversalPath(entry.getKey());
            if (tp.isPresent()) {
                logger.debug("Adding traversal order: {}", entry.getKey());
                query = query.orderByTraversal(tp.get(), entry.getValue());
            } else {
                logger.debug("Adding property order: {}", entry.getKey());
                query = query.orderBy(entry.getKey(), entry.getValue());
            }
        }
        return query;
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
                traversalSort,
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
                traversalSort, defaultSort, filters, tmp, traversalFilters, page);
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
        ImmutableSortedMap.Builder<String, Pair<FilterPredicate, String>> builder
                = new ImmutableSortedMap.Builder<String, Pair<FilterPredicate, String>>(
                    Ordering.natural());
        for (Entry<String, Pair<FilterPredicate,String>> filter : filters.entrySet()) {
            if (!filter.getKey().equals(property)) {
                builder.put(filter.getKey(), filter.getValue());
            }
        }
        builder.put(property, new Pair<FilterPredicate, String>(predicate, value));
        
        return new Query<E>(graph, cls, scope, offset, limit, sort,
                traversalSort, defaultSort, builder.build(), depthFilters, traversalFilters, page);
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
        ArrayList<GremlinPipeline<Vertex, Vertex>> tmp = Lists.newArrayList(traversalFilters);
        tmp.add(getFilterTraversalPipeline(path, new Pair<FilterPredicate, String>(predicate, value)));
        return new Query<E>(graph, cls, scope, offset, limit, sort,
                traversalSort, defaultSort, filters, depthFilters, tmp, page);
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
        SortedMap<String, Pair<FilterPredicate, String>> tmp = QueryUtils.parseFilters(filters);
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

    // Helpers

    private <EE> GremlinPipeline<EE, Vertex> setPipelineRange(
            GremlinPipeline<EE, Vertex> filter) {
        int low = Math.max(offset.or(0), 0);
        int high = low + Math.max(limit.or(-1), 0) - 1;
        return filter.range(low, high);
    }

    private <EE> GremlinPipeline<EE, Vertex> setOrder(
            GremlinPipeline<EE, Vertex> pipe) {
        pipe = setTraversalOrdering(pipe);
        if (sort.isEmpty()) {
            if (defaultSort.isPresent()) {
                return pipe
                        .order(getOrderFunction(new ImmutableSortedMap.Builder<String, Sort>(
                                Ordering.natural().nullsLast()).put(
                                defaultSort.get().getA(),
                                defaultSort.get().getB()).build()));
            }
            return pipe;
        }
        return pipe.order(getOrderFunction(sort));
    }

    private <EE> GremlinPipeline<EE, Vertex> setTraversalOrdering(
            GremlinPipeline<EE, Vertex> pipe) {
        for (Entry<QueryUtils.TraversalPath, Sort> entry : traversalSort.entrySet()) {
            pipe = pipe.order(getTraversalOrderFunction(entry.getKey(), entry.getValue()));
        }
        return pipe;
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

    /**
     * Get a PipeFunction which sorted vertices by the ordered list
     * of sort parameters.
     *
     * @param sort
     * @return
     */
    private PipeFunction<Pair<Vertex, Vertex>, Integer> getOrderFunction(
            final SortedMap<String, Sort> sort) {
        final Ordering<Comparable<?>> order = Ordering.natural().nullsLast();
        return new PipeFunction<Pair<Vertex, Vertex>, Integer>() {
            public Integer compute(Pair<Vertex, Vertex> pair) {
                ComparisonChain chain = ComparisonChain.start();
                for (Entry<String, Sort> entry : sort.entrySet()) {
                    Vertex a = entry.getValue() == Sort.ASC ? pair.getA() : pair.getB();
                    Vertex b = entry.getValue() == Sort.ASC ? pair.getB() : pair.getA();
                    chain = chain.compare(
                            (String) a.getProperty(entry.getKey()),
                            (String) b.getProperty(entry.getKey()), order);
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

    /**
     * Create a function that filters nodes given a string and a predicate.
     * @return
     */
    private PipeFunction<Vertex, Boolean> getFilterFunction() {
        return new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex vertex) {
                for (Entry<String, Pair<FilterPredicate, String>> entry : filters
                        .entrySet()) {
                    String p = (String) vertex.getProperty(entry.getKey());
                    if (p == null || !matches(p, entry.getValue()
                            .getB(), entry.getValue().getA())) {
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
                for (GremlinPipeline<Vertex, Vertex> pipeline : traversalFilters) {
                    pipeline.reset();
                    if (!pipeline.start(vertex).hasNext()) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    private PipeFunction<Pair<Vertex, Vertex>, Integer> getTraversalOrderFunction(
            final QueryUtils.TraversalPath tp, final Sort sort) {
        // FIXME: Make this less horribly inefficient!
        final GremlinPipeline<Vertex, String> pipe = getOrderTraversalPipeline(tp);
        final Map<Vertex, String> cache = Maps.newHashMap();
        final Ordering<Comparable<?>> order = Ordering.natural().nullsLast();
        return new PipeFunction<Pair<Vertex, Vertex>, Integer>() {
            public Integer compute(Pair<Vertex, Vertex> pair) {
                String a = cache.get(pair.getA());
                String b = cache.get(pair.getB());
                if (!cache.containsKey(pair.getA())) {
                    pipe.reset();
                    if (pipe.start(pair.getA()).hasNext()) {
                        a = pipe.next();
                        cache.put(pair.getA(), a);
                    }
                }
                if (!cache.containsKey(pair.getB())) {
                    pipe.reset();
                    if (pipe.start(pair.getB()).hasNext()) {
                        b = pipe.next();
                        cache.put(pair.getB(), b);
                    }
                }
                return sort == Sort.ASC ? order.compare(a, b) : order.compare(b, a);
            }
        };
    }

    private GremlinPipeline<Vertex, String> getOrderTraversalPipeline(
            final QueryUtils.TraversalPath traversalPath) {
        GremlinPipeline<Vertex, Vertex> p = addTraversals(traversalPath.getTraversals(), false,
                new GremlinPipeline<Vertex, Vertex>());
        return p.transform(new PipeFunction<Vertex, String>() {
            public String compute(Vertex vertex) {
                return (String) vertex.getProperty(traversalPath.getProperty());
            }
        });
    }


    private GremlinPipeline<Vertex, Vertex> getFilterTraversalPipeline(
            final QueryUtils.TraversalPath traversalPath,
            final Pair<FilterPredicate, String> filter) {
        GremlinPipeline<Vertex, Vertex> p = addTraversals(traversalPath.getTraversals(), false,
                new GremlinPipeline<Vertex, Vertex>());
        return p.filter(new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex vertex) {
                String p = (String) vertex.getProperty(traversalPath.getProperty());
                return p != null && matches(p, filter.getB(), filter.getA());
            }
        });
    }

    /**
     * Add traversals to a pipeline given a set of String/Direction pairs.
     * @param paths
     * @param reverse   reverse the traversal ordering
     * @param pipe      the pipeline to process
     * @param <S>
     * @return
     */
    private static <S> GremlinPipeline<S, Vertex> addTraversals(List<Pair<String, Direction>> paths,
            Boolean reverse, GremlinPipeline<S, Vertex> pipe) {
        List<Pair<String, Direction>> orderedPaths = reverse ? Lists.reverse(paths) : paths;
        for (Pair<String, Direction> tp : orderedPaths) {
            Direction direction = reverse ? tp.getB().opposite() : tp.getB();
            String relation = tp.getA();
            switch (direction) {
                case IN:
                    logger.debug("Adding {} relation: {}", relation, direction);
                    pipe = pipe.in(relation);
                    break;
                case OUT:
                    logger.debug("Adding {} relation: {}", relation, direction);
                    pipe = pipe.out(relation);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected direction in traversal pipe function: " + tp.getB());
            }
        }
        return pipe;
    }


    // FIXME: This has several limitations so far.
    // - only handles properties cast as strings
    // - doesn't do case-insensitive regexp matching.
    private boolean matches(String a, String b, FilterPredicate predicate) {
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
