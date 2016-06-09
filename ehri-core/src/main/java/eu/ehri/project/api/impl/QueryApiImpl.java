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

package eu.ehri.project.api.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.util.structures.Pair;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.api.QueryApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;

/**
 * Handles querying Entity entities with ACL semantics.
 */
public final class QueryApiImpl implements QueryApi {
    private static final long NO_COUNT = -1L;

    private final int offset;
    private final int limit;
    private final SortedMap<String, Sort> sort;
    private final Optional<Pair<String, Sort>> defaultSort;
    private final SortedMap<String, Pair<FilterPredicate, Object>> filters;
    private final boolean stream;

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final Accessor accessor;

    /**
     * Full Constructor.
     */
    public QueryApiImpl(
            FramedGraph<?> graph,
            Accessor accessor,
            int offset,
            int limit,
            SortedMap<String, Sort> sort,
            Optional<Pair<String, Sort>> defSort,
            SortedMap<String, Pair<FilterPredicate, Object>> filters,
            boolean stream) {
        this.graph = graph;
        this.accessor = accessor;
        this.offset = offset;
        this.limit = limit;
        this.sort = ImmutableSortedMap.copyOf(sort);
        this.defaultSort = defSort;
        this.filters = ImmutableSortedMap.copyOf(filters);
        this.stream = stream;
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Simple constructor.
     */
    public QueryApiImpl(FramedGraph<?> graph, Accessor accessor) {
        this(graph, accessor, 0, DEFAULT_LIMIT,
                ImmutableSortedMap.<String, Sort>of(), Optional
                        .<Pair<String, Sort>>absent(), ImmutableSortedMap
                        .<String, Pair<FilterPredicate, Object>>of(), false);
    }

    /**
     * Query builder.
     */
    public static class Builder {
        private FramedGraph<?> graph;
        private Accessor accessor;
        private int offset;
        private int limit = DEFAULT_LIMIT;
        private SortedMap<String, Sort> sort = ImmutableSortedMap.of();
        private Optional<Pair<String, Sort>> defSort = Optional.absent();
        private SortedMap<String, Pair<FilterPredicate, Object>> filters = ImmutableSortedMap.of();
        private boolean stream;

        Builder setSort(SortedMap<String, Sort> sort) {
            this.sort = sort;
            return this;
        }

        Builder(QueryApiImpl query) {
            this(query.graph, query.accessor);
            this.offset = query.offset;
            this.limit = query.limit;
            this.sort = query.sort;
            this.defSort = query.defaultSort;
            this.filters = query.filters;
            this.stream = query.stream;
        }

        Builder setFilters(SortedMap<String, Pair<FilterPredicate, Object>> filters) {
            this.filters = filters;
            return this;
        }

        public Builder(FramedGraph<?> graph, Accessor accessor) {
            this.graph = graph;
            this.accessor = accessor;
        }

        public Builder setOffset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder setSort(Collection<String> orderSpecs) {
            this.sort = QueryUtils.parseOrderSpecs(orderSpecs);
            return this;
        }

        public Builder setFilters(Collection<String> filters) {
            this.filters = QueryUtils.parseFilters(filters);
            return this;
        }

        public Builder setStream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public QueryApiImpl build() {
            return new QueryApiImpl(graph, accessor, offset, limit, sort, defSort, filters, stream);
        }
    }

    @Override
    public QueryApiImpl setOffset(int offset) {
        return new Builder(this).setOffset(offset).build();
    }

    @Override
    public QueryApiImpl setLimit(int limit) {
        return new Builder(this).setLimit(limit).build();
    }

    @Override
    public QueryApiImpl filter(String key, FilterPredicate predicate, Object value) {
        ImmutableSortedMap.Builder<String, Pair<FilterPredicate, Object>> m =
                new ImmutableSortedMap.Builder<>(Ordering.natural());
        m.putAll(filters);
        m.put(key, new Pair<>(predicate, value));
        return new Builder(this).setFilters(m.build()).build();
    }

    @Override
    public QueryApiImpl filter(Collection<String> filterSpecs) {
        return new Builder(this).setFilters(filterSpecs).build();
    }

    @Override
    public QueryApiImpl orderBy(String key, Sort order) {
        SortedMap<String, Sort> sm = Maps.newTreeMap();
        sm.putAll(sort);
        sm.put(key, order);
        return new Builder(this).setSort(sm).build();
    }

    @Override
    public QueryApiImpl orderBy(Collection<String> orderSpecs) {
        return new Builder(this).setSort(orderSpecs).build();
    }

    @Override
    public QueryApiImpl setStream(boolean stream) {
        return new Builder(this).setStream(stream).build();
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
    @Override
    public <E extends Entity> Page<E> page(Class<E> cls) {
        return page(ClassUtils.getEntityType(cls), cls);
    }

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    @Override
    public <E extends Entity> Page<E> page(EntityClass type, Class<E> cls) {
        return page(manager.getEntities(type, cls), cls);
    }

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    @Override
    public <E extends Entity> Page<E> page(Iterable<E> vertices, Class<E> cls) {
        PipeFunction<Vertex, Boolean> aclFilterFunction = AclManager
                .getAclFilterFunction(accessor);

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
            Iterable<E> iterable = graph.frameVertices(
                    setPipelineRange(setOrder(new GremlinPipeline<Vertex, Vertex>(
                            userVerts))), cls);
            return new Page<>(iterable, offset, limit, userVerts.size());
        }
    }

    /**
     * Return a Page instance containing a total of total items, and an iterable
     * for the given page/count.
     */
    @Override
    public <E extends Entity> Page<E> page(String key, String query, Class<E> cls) {
        try (CloseableIterable<Vertex> countQ = manager.getVertices(key, query,
                ClassUtils.getEntityType(cls))) {
            try (CloseableIterable<Vertex> indexQ = manager.getVertices(key,
                    query, ClassUtils.getEntityType(cls))) {
                PipeFunction<Vertex, Boolean> aclFilterFunction = AclManager.getAclFilterFunction(accessor);
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
     * Count items accessible to a given user.
     * <p/>
     * NB: Count doesn't 'account' for ACL privileges!
     */
    @Override
    public long count(Iterable<?> vertices) {
        GremlinPipeline<?, Vertex> filter = new GremlinPipeline<>(vertices);
        return setFilters(filter).count();
    }

    /**
     * Count all items of a given type.
     * <p/>
     * NB: Count doesn't 'account' for ACL privileges!
     */
    @Override
    public long count(EntityClass type) {
        return count(manager.getVertices(type));
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

    private <EE> GremlinPipeline<EE, Vertex> setFilters(GremlinPipeline<EE, Vertex> pipe) {
        return filters.isEmpty() ? pipe : pipe.filter(getFilterFunction());
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
                for (Entry<String, Pair<FilterPredicate, Object>> entry : filters
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
    private boolean matches(String a, Object b, FilterPredicate predicate) {
        switch (predicate) {
            case EQUALS:
                return a.equals(b);
            case IEQUALS:
                return a.equalsIgnoreCase(b.toString());
            case STARTSWITH:
                return a.startsWith(b.toString());
            case ENDSWITH:
                return a.endsWith(b.toString());
            case CONTAINS:
                return a.contains(b.toString());
            case ICONTAINS:
                return a.toLowerCase().contains(b.toString().toLowerCase());
            case MATCHES:
                return a.matches(b.toString());
            case GT:
                return a.compareTo(b.toString()) > 0;
            case GTE:
                return a.compareTo(b.toString()) >= 0;
            case LT:
                return a.compareTo(b.toString()) < 0;
            case LTE:
                return a.compareTo(b.toString()) <= 0;
            default:
                throw new RuntimeException("Unexpected filter predicate: "
                        + predicate);
        }
    }
}
