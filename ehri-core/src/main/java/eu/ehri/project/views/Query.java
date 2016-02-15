/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.views;

import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.util.structures.Pair;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;

/**
 * Handles querying Accessible Entities, with ACL semantics.
 * <p/>
 * TODO: Possibly refactor more of the ACL logic into AclManager.
 *
 * @param <E>
 */
public final class Query<E extends Accessible> implements Scoped<Query> {

    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 20;
    private static final long NO_COUNT = -1L;


    private static final Logger logger = LoggerFactory.getLogger(Query.class);

    private final int offset;
    private final int limit;
    private final SortedMap<String, Sort> sort;
    private final Optional<Pair<String, Sort>> defaultSort;
    private final SortedMap<String, Pair<FilterPredicate, String>> filters;
    private final boolean stream;

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final Class<E> cls;
    private final PermissionScope scope;

    /**
     * Directions for sort.
     */
    public enum Sort {
        ASC, DESC
    }

    /**
     * Filter predicates
     */
    public enum FilterPredicate {
        EQUALS, IEQUALS, STARTSWITH, ENDSWITH, CONTAINS, ICONTAINS, MATCHES, GT, GTE, LT, LTE
    }

    /**
     * Full Constructor.
     */
    private Query(
            FramedGraph<?> graph, Class<E> cls,
            PermissionScope scope,
            int offset,
            int limit,
            SortedMap<String, Sort> sort,
            Optional<Pair<String, Sort>> defSort,
            SortedMap<String, Pair<FilterPredicate, String>> filters,
            boolean stream) {
        this.graph = graph;
        this.cls = cls;
        this.scope = scope;
        this.offset = offset;
        this.limit = limit;
        this.sort = ImmutableSortedMap.copyOf(sort);
        this.defaultSort = defSort;
        this.filters = ImmutableSortedMap
                .copyOf(filters);
        this.stream = stream;
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Simple constructor.
     */
    public Query(FramedGraph<?> graph, Class<E> cls) {
        this(graph, cls, SystemScope.getInstance(),
                DEFAULT_OFFSET, DEFAULT_LIMIT,
                ImmutableSortedMap.<String, Sort>of(), Optional
                        .<Pair<String, Sort>>absent(), ImmutableSortedMap
                        .<String, Pair<FilterPredicate, String>>of(), false);
    }

    /**
     * Copy constructor.
     */
    public Query<E> copy(Query<E> other) {
        return new Query<>(other.graph, other.cls, other.scope, other.offset,
                other.limit, other.sort, other.defaultSort, other.filters,
                other.stream);
    }


    /**
     * Class representing a page of content.
     *
     * @param <T> the item type
     */
    public static class Page<T> implements Iterable<T> {
        private final Iterable<T> iterable;
        private final int page;
        private final int count;
        private final long total;

        public Page(Iterable<T> iterable, int page, int count, long total) {
            this.iterable = iterable;
            this.total = total;
            this.page = page;
            this.count = count;
        }

        public Iterable<T> getIterable() {
            return iterable;
        }

        public long getTotal() {
            return total;
        }

        public Integer getOffset() {
            return page;
        }

        public Integer getLimit() {
            return count;
        }

        @Override
        public Iterator<T> iterator() {
            return iterable.iterator();
        }

        @Override
        public String toString() {
            return String.format("<Page[...] %d %d (%d)", page, count, total);
        }
    }

    /**
     * Wrapper method for FramedVertexIterables that converts a
     * {@code FramedVertexIterable<T>} back into a plain {@code Iterable<Vertex>}.
     *
     * @param <T>
     */
    public static class FramedVertexIterableAdaptor<T extends Entity>
            implements Iterable<Vertex> {
        final Iterable<T> iterable;

        public FramedVertexIterableAdaptor(Iterable<T> iterable) {
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
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    public Page<E> page(Accessor user) {
        return page(ClassUtils.getEntityType(cls), user);
    }

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    public Page<E> page(EntityClass type, Accessor user) {
        return page(manager.getEntities(type, cls), user);
    }

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    public Page<E> page(Iterable<E> vertices, Accessor user) {
        return page(vertices, user, cls);
    }

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    public <T extends Entity> Page<T> page(Iterable<T> vertices, Accessor user, Class<T> cls) {
        PipeFunction<Vertex, Boolean> aclFilterFunction = AclManager
                .getAclFilterFunction(user);

        GremlinPipeline<E, Vertex> pipeline = new GremlinPipeline<E, Vertex>(
                new FramedVertexIterableAdaptor<>(vertices))
                .filter(aclFilterFunction);

        if (stream) {
            return new Page<>(graph.frameVertices(
                    setPipelineRange(setOrder(setFilters(pipeline))), cls), offset, limit, NO_COUNT);
        } else {
            // FIXME: We have to read the vertices into memory here since we
            // can't re-use the iterator for counting and streaming.
            ArrayList<Vertex> userVerts = Lists.newArrayList(setFilters(pipeline).iterator());
            Iterable<T> iterable = graph.frameVertices(
                    setPipelineRange(setOrder(new GremlinPipeline<Vertex, Vertex>(
                            userVerts))), cls);
            return new Page<>(iterable, offset, limit, userVerts.size());
        }
    }

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    public Page<E> page(String key, String query, Accessor user) {
        try (CloseableIterable<Vertex> countQ = manager.getVertices(key, query,
                ClassUtils.getEntityType(cls))) {
            try (CloseableIterable<Vertex> indexQ = manager.getVertices(key,
                    query, ClassUtils.getEntityType(cls))) {
                PipeFunction<Vertex, Boolean> aclFilterFunction = AclManager.getAclFilterFunction(user);
                long numItems = stream
                        ? NO_COUNT
                        : setFilters(new GremlinPipeline<Vertex, Vertex>(countQ)
                        .filter(aclFilterFunction)).count();

                return new Page<>(
                        graph.frameVertices(
                                setPipelineRange(setOrder(setFilters(new GremlinPipeline<Vertex, Vertex>(
                                        indexQ).filter(aclFilterFunction)))),
                                cls), offset, limit, numItems);
            }
        }
    }

    /**
     * Count items.
     * <p/>
     * NB: Count doesn't 'account' for ACL privileges!
     */
    public long count() {
        return count(ClassUtils.getEntityType(cls));
    }

    /**
     * Count items accessible to a given user.
     * <p/>
     * NB: Count doesn't 'account' for ACL privileges!
     */
    public <T> long count(Iterable<T> vertices) {
        GremlinPipeline<T, Vertex> filter = new GremlinPipeline<>(vertices);
        return setFilters(filter).count();
    }

    /**
     * Count all items of a given type.
     * <p/>
     * NB: Count doesn't 'account' for ACL privileges!
     */
    public long count(EntityClass type) {
        return count(manager.getVertices(type));
    }

    /**
     * Set the page applied to this query.
     *
     * @param offset An integer page to the stream.
     */
    public Query<E> setOffset(int offset) {
        return new Query<>(graph, cls, scope, offset,
                limit, sort, defaultSort, filters, stream);
    }

    /**
     * Set the total applied to this query.
     *
     * @param limit An integer total, or -1 for an unbounded stream.
     */
    public Query<E> setLimit(int limit) {
        return new Query<>(graph, cls, scope, offset,
                limit, sort, defaultSort, filters, stream);
    }

    /**
     * Indicate that we want a stream of results and therefore
     * don't care about the item total (which will be -1 as a
     * result).
     *
     * @param stream Whether to stream results lazily.
     */
    public Query<E> setStream(boolean stream) {
        return new Query<>(graph, cls, scope, offset,
                limit, sort, defaultSort, filters, stream);
    }

    /**
     * Add an default order clause, to be used if no custom order is specified.
     */
    public Query<E> defaultOrderBy(String field, Sort order) {

        return new Query<>(graph, cls, scope, offset, limit, sort,
                Optional.of(new Pair<>(field, order)), filters, stream);
    }

    /**
     * Add an order clause.
     */
    public Query<E> orderBy(String field, Sort order) {
        SortedMap<String, Sort> tmp = new ImmutableSortedMap.Builder<String, Sort>(
                Ordering.natural()).putAll(sort).put(field, order).build();
        return new Query<>(graph, cls, scope, offset, limit, tmp, defaultSort,
                filters, stream);
    }

    /**
     * Add a set of string order clauses. Clauses must be of the form:
     * <p/>
     * property__DIRECTION
     *
     * @param orderSpecs list of orderSpecs
     */
    public Query<E> orderBy(Collection<String> orderSpecs) {
        SortedMap<String, Sort> tmp = QueryUtils.parseOrderSpecs(orderSpecs);
        Query<E> query = this;
        for (Entry<String, Sort> entry : tmp.entrySet()) {
            logger.debug("Adding property order: {}", entry.getKey());
            query = query.orderBy(entry.getKey(), entry.getValue());
        }
        return query;
    }

    /**
     * Clear the filter clauses from this query.
     */
    public Query<E> clearFilters() {
        return new Query<>(
                graph,
                cls,
                scope,
                offset,
                limit,
                sort,
                defaultSort,
                ImmutableSortedMap.<String, Pair<FilterPredicate, String>>of(),
                stream);
    }

    /**
     * Clear the filter clauses from this query.
     */
    public Query<E> clearOrdering() {
        return new Query<>(
                graph,
                cls,
                scope,
                offset,
                limit,
                ImmutableSortedMap.<String, Sort>of(),
                defaultSort,
                filters,
                stream);
    }

    /**
     * Add a filter clause.
     */
    public Query<E> filter(String property, FilterPredicate predicate, String value) {
        ImmutableSortedMap.Builder<String, Pair<FilterPredicate, String>> builder
                = new ImmutableSortedMap.Builder<>(
                Ordering.natural());
        for (Entry<String, Pair<FilterPredicate, String>> filter : filters.entrySet()) {
            if (!filter.getKey().equals(property)) {
                builder.put(filter.getKey(), filter.getValue());
            }
        }
        builder.put(property, new Pair<>(predicate, value));

        return new Query<>(graph, cls, scope, offset, limit, sort,
                defaultSort, builder.build(), stream);
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
    public Query<E> filter(Collection<String> filters) {
        SortedMap<String, Pair<FilterPredicate, String>> tmp = QueryUtils.parseFilters(filters);
        Query<E> query = this;
        for (Entry<String, Pair<FilterPredicate, String>> ft : tmp.entrySet()) {
            logger.debug("Adding property filter: {}", ft.getKey());
            query = query.filter(ft.getKey(), ft.getValue().getA(), ft.getValue().getB());
        }
        return query;
    }

    // Helpers

    private <EE> GremlinPipeline<EE, Vertex> setPipelineRange(
            GremlinPipeline<EE, Vertex> filter) {
        int low = Math.max(0, offset);
        if (limit < 0) {
            // No way to skip a bunch of items in Gremlin without
            // applying an end range... I guess this will break if
            // we have more than 2147483647 items to traverse...
            return filter.range(low, Integer.MAX_VALUE); // No filtering
        } else if (limit == 0) {
            return new GremlinPipeline<>(Lists.newArrayList());
        } else {
            // NB: The high range is inclusive, oddly.
            return filter.range(low, low + (limit - 1));
        }
    }

    private <EE> GremlinPipeline<EE, Vertex> setOrder(
            GremlinPipeline<EE, Vertex> pipe) {
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

    private <EE> GremlinPipeline<EE, Vertex> setFilters(
            GremlinPipeline<EE, Vertex> pipe) {
        if (filters.isEmpty())
            return pipe;
        return pipe.filter(getFilterFunction());
    }

    /**
     * Get a PipeFunction which sorted vertices by the ordered list
     * of sort parameters.
     */
    private PipeFunction<Pair<Vertex, Vertex>, Integer> getOrderFunction(final SortedMap<String, Sort> sort) {
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
     * Create a function that filters nodes given a string and a predicate.
     */
    private PipeFunction<Vertex, Boolean> getFilterFunction() {
        return new PipeFunction<Vertex, Boolean>() {
            public Boolean compute(Vertex vertex) {
                for (Entry<String, Pair<FilterPredicate, String>> entry : filters
                        .entrySet()) {
                    String p = vertex.getProperty(entry.getKey());
                    if (p == null || !matches(p, entry.getValue()
                            .getB(), entry.getValue().getA())) {
                        return false;
                    }
                }
                return true;
            }
        };
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

    @Override
    public Query withScope(PermissionScope scope) {
        return new Query<>(
                graph,
                cls,
                scope,
                offset,
                limit,
                sort,
                defaultSort,
                filters,
                stream);
    }
}
