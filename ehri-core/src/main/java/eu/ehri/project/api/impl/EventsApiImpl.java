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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.api.EventsApi;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.Watchable;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.utils.pipes.AggregatorPipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * View class for handling event streams.
 */
public class EventsApiImpl implements eu.ehri.project.api.EventsApi {

    private static final Logger logger = LoggerFactory.getLogger(EventsApiImpl.class);

    // Maximum number of events to be aggregated in a single batch
    private static final int MAX_AGGREGATION = 200;

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final Accessor accessor;
    private final ActionManager actionManager;
    private final int offset;
    private final int limit;
    private final Set<String> users;
    private final Set<String> ids;
    private final Set<EntityClass> entityTypes;
    private final Set<EventTypes> eventTypes;
    private final Optional<String> from;
    private final Optional<String> to;
    private final Set<ShowType> showType;
    private final Aggregation aggregation;

    // Aggregator function that aggregates adjacent events by 'strict' similarity,
    // which in practice means:
    // - same event type
    // - same subject(s)
    // - same actioner
    private static final AggregatorPipe.AggregatorFunction<SystemEvent> strictAggregator =
            (a, b, count) -> ActionManager.sameAs(a, b);

    // Aggregator function that aggregates adjacent events by actioner
    private static final AggregatorPipe.AggregatorFunction<SystemEvent> userAggregator =
            (a, b, count) -> count < MAX_AGGREGATION
                    && ActionManager.sequentialWithSameAccessor(b, a);

    public static class Builder {
        private final FramedGraph<?> graph;
        private final Accessor accessor;
        private int offset = -1;
        private int limit = -1;
        private Set<String> users = Sets.newHashSet();
        private Set<String> ids = Sets.newHashSet();
        private Set<EntityClass> entityTypes = Sets.newHashSet();
        private Set<EventTypes> eventTypes = Sets.newHashSet();
        private Optional<String> from = Optional.empty();
        private Optional<String> to = Optional.empty();
        private Set<ShowType> showType = Sets.newHashSet();
        private Aggregation aggregation = Aggregation.strict;

        public Builder(FramedGraph<?> graph, Accessor accessor) {
            this.graph = graph;
            this.accessor = accessor;
        }

        private Builder(EventsApiImpl eventsApi) {
            this.graph = eventsApi.graph;
            this.accessor = eventsApi.accessor;
            this.offset = eventsApi.offset;
            this.limit = eventsApi.limit;
            this.users = eventsApi.users;
            this.ids = eventsApi.ids;
            this.entityTypes = eventsApi.entityTypes;
            this.eventTypes = eventsApi.eventTypes;
            this.from = eventsApi.from;
            this.to = eventsApi.to;
            this.showType = eventsApi.showType;
            this.aggregation = eventsApi.aggregation;
        }

        public Builder withRange(int offset, int limit) {
            this.offset = offset;
            this.limit = limit;
            return this;
        }

        public Builder withUsers(String... users) {
            this.users.addAll(Lists.newArrayList(users));
            return this;
        }

        public Builder withIds(String... ids) {
            this.ids.addAll(Lists.newArrayList(ids));
            return this;
        }

        public Builder withEntityTypes(EntityClass... entities) {
            this.entityTypes.addAll(Lists.newArrayList(entities));
            return this;
        }

        public Builder withEventTypes(EventTypes... eventTypes) {
            this.eventTypes.addAll(Lists.newArrayList(eventTypes));
            return this;
        }

        public Builder from(String from) {
            this.from = Optional.ofNullable(from);
            return this;
        }

        public Builder to(String to) {
            this.to = Optional.ofNullable(to);
            return this;
        }

        public Builder withShowType(ShowType... showTypes) {
            this.showType.addAll(Lists.newArrayList(showTypes));
            return this;
        }

        public Builder withAggregation(Aggregation aggregation) {
            this.aggregation = aggregation;
            return this;
        }

        public EventsApi build() {
            return new EventsApiImpl(
                    graph,
                    accessor,
                    offset,
                    limit,
                    users,
                    ids,
                    entityTypes,
                    eventTypes,
                    from,
                    to,
                    showType,
                    aggregation
            );
        }
    }

    private EventsApiImpl(
            FramedGraph<?> graph,
            Accessor accessor,
            int offset,
            int limit,
            Collection<String> users,
            Collection<String> ids,
            Collection<EntityClass> entityTypes,
            Collection<EventTypes> eventTypes,
            Optional<String> from,
            Optional<String> to,
            Collection<ShowType> showType,
            Aggregation aggregation) {
        this.graph = graph;
        this.accessor = accessor;
        this.actionManager = new ActionManager(graph);
        this.manager = GraphManagerFactory.getInstance(graph);
        this.offset = offset;
        this.limit = limit;
        this.users = Sets.newHashSet(users);
        this.ids = Sets.newHashSet(ids);
        this.entityTypes = Sets.newEnumSet(entityTypes, EntityClass.class);
        this.eventTypes = Sets.newEnumSet(eventTypes, EventTypes.class);
        this.from = from;
        this.to = to;
        this.showType = Sets.newEnumSet(showType, ShowType.class);
        this.aggregation = aggregation;

    }

    public EventsApiImpl(FramedGraph<?> graph, Accessor accessor) {
        this(graph, accessor,
                -1,
                -1,
                Lists.<String>newArrayList(),
                Lists.<String>newArrayList(),
                Lists.<EntityClass>newArrayList(),
                Lists.<EventTypes>newArrayList(),
                Optional.empty(),
                Optional.empty(),
                Lists.<ShowType>newArrayList(),
                Aggregation.strict);
    }

    @Override
    public Iterable<SystemEvent> list() {
        // Add optional filters for event type, item type, and asUser...
        GremlinPipeline<SystemEvent, SystemEvent> pipe = new GremlinPipeline<>(initStream());

        // Add additional generic filters
        return setPipelineRange(applyAclFilter(filterEvents(pipe), accessor));
    }

    @Override
    public Iterable<List<SystemEvent>> aggregate() {
        // Add optional filters for event type, item type, and asUser...
        GremlinPipeline<SystemEvent, SystemEvent> pipe = new GremlinPipeline<>(initStream());

        // Add additional generic filters
        GremlinPipeline<SystemEvent, SystemEvent> aclFiltered =
                applyAclFilter(filterEvents(pipe), accessor);
        return setPipelineRange(aggregateFilter(aclFiltered));
    }

    /**
     * List items "interesting" for a user, optionally filtered by the `ShowType`
     * to items they watch or users they follow.
     */
    @Override
    public Iterable<SystemEvent> listAsUser(UserProfile asUser) {
        GremlinPipeline<SystemEvent, SystemEvent> pipe = getPersonalisedEvents(asUser, accessor);
        return setPipelineRange(pipe);
    }

    /**
     * List events "interesting" for a user, with aggregation.
     */
    @Override
    public Iterable<List<SystemEvent>> aggregateAsUser(UserProfile asUser) {
        GremlinPipeline<SystemEvent, SystemEvent> pipe = getPersonalisedEvents(asUser, accessor);
        return setPipelineRange(aggregateFilter(pipe));
    }

    /**
     * List an item's events.
     */
    @Override
    public Iterable<SystemEvent> listForItem(Accessible item) {
        // Add optional filters for event type, item type, and asUser...
        GremlinPipeline<SystemEvent, SystemEvent> pipe = new GremlinPipeline<>(item.getHistory());
        // Add additional generic filters
        GremlinPipeline<SystemEvent, SystemEvent> acl = applyAclFilter(filterEvents(pipe), accessor);
        return setPipelineRange(acl);
    }

    /**
     * Aggregate an item's events.
     */
    @Override
    public Iterable<List<SystemEvent>> aggregateForItem(Accessible item) {
        // Add optional filters for event type, item type, and asUser...
        GremlinPipeline<SystemEvent, SystemEvent> pipe = new GremlinPipeline<>(item.getHistory());
        // Add additional generic filters
        GremlinPipeline<SystemEvent, SystemEvent> acl = applyAclFilter(filterEvents(pipe), accessor);
        return setPipelineRange(aggregateFilter(acl));
    }

    /**
     * Aggregate a user's actions.
     *
     * @param byUser the user
     * @return an event stream
     */
    @Override
    public Iterable<List<SystemEvent>> aggregateUserActions(UserProfile byUser) {
        // Add optional filters for event type, item type, and asUser...
        GremlinPipeline<SystemEvent, SystemEvent> pipe = new GremlinPipeline<>(
                byUser.as(Actioner.class).getActions());

        // Add additional generic filters
        GremlinPipeline<SystemEvent, SystemEvent> acl = applyAclFilter(filterEvents(pipe), accessor);
        return setPipelineRange(aggregateFilter(acl));
    }

    /**
     * List a user's actions.
     *
     * @param byUser the user
     * @return an event stream
     */
    @Override
    public Iterable<SystemEvent> listByUser(UserProfile byUser) {
        // Add optional filters for event type, item type, and asUser...
        GremlinPipeline<SystemEvent, SystemEvent> pipe = new GremlinPipeline<>(
                byUser.as(Actioner.class).getActions());

        // Add additional generic filters
        return setPipelineRange(applyAclFilter(filterEvents(pipe), accessor));
    }

    // Helpers

    private Iterable<SystemEvent> initStream() {
        // If we're filtering the list for specific user's actions
        // it's much more efficient to aggregate the user(s) event
        // streams directly via than scanning the global one, even
        // though we then have to sort the events to combine them
        // into a newest-first stream.
        if (users.isEmpty() && ids.isEmpty()) {
            // No item/user filter: scan the global queue...
            return actionManager.getLatestGlobalEvents();
        } else {
            List<Actioner> actioners = getItems(users, Actioner.class);
            List<Accessible> entities = getItems(ids, Accessible.class);
            if (actioners.size() == 1) {
                // Single user: return user's action queue
                return actioners.get(0).getActions();
            } else if (entities.size() == 1) {
                // Single item: return item's history queue
                return entities.get(0).getHistory();
            } else if (actioners.size() > 1) {
                // Merge multiple user action queues
                List<Iterable<SystemEvent>> actions = Lists.newArrayList();
                for (Actioner actioner : actioners) {
                    actions.add(actioner.getActions());
                }
                return mergeEventQueues(actions);
            } else {
                // Merge multiple item history queues
                List<Iterable<SystemEvent>> histories = Lists.newArrayList();
                for (Accessible entity : entities) {
                    histories.add(entity.getHistory());
                }
                return mergeEventQueues(histories);
            }
        }
    }

    private Iterable<SystemEvent> mergeEventQueues(List<Iterable<SystemEvent>> queues) {
        return Iterables.mergeSorted(queues,
                (event1, event2) -> event2.getTimestamp().compareTo(event1.getTimestamp()));
    }

    private GremlinPipeline<SystemEvent, SystemEvent> applyAclFilter(GremlinPipeline<SystemEvent, SystemEvent> pipe,
            Accessor asUser) {
        final PipeFunction<Vertex, Boolean> aclFilterTest = AclManager.getAclFilterFunction(asUser);

        // Filter items accessible to this asUser... hide the
        // event if any subjects or the scope are inaccessible
        // to the asUser.
        return pipe.filter(event -> {
            Entity eventScope = event.getEventScope();
            if (eventScope != null && !aclFilterTest.compute(eventScope.asVertex())) {
                return false;
            }
            for (Accessible e : event.getSubjects()) {
                if (!aclFilterTest.compute(e.asVertex())) {
                    return false;
                }
            }
            return true;
        });
    }

    private GremlinPipeline<SystemEvent, SystemEvent> filterEvents(
            GremlinPipeline<SystemEvent, SystemEvent> pipe) {

        if (!eventTypes.isEmpty()) {
            pipe = pipe.filter(event -> eventTypes.contains(event.getEventType()));
        }

        if (!ids.isEmpty()) {
            pipe = pipe.filter(event -> {
                for (Accessible e : event.getSubjects()) {
                    if (ids.contains(e.getId())) {
                        return true;
                    }
                }
                return false;
            });
        }

        if (!entityTypes.isEmpty()) {
            pipe = pipe.filter(event -> {
                for (Accessible e : event.getSubjects()) {
                    if (entityTypes.contains(manager.getEntityClass(e))) {
                        return true;
                    }
                }
                return false;
            });
        }

        if (!users.isEmpty()) {
            pipe = pipe.filter(event -> {
                Actioner actioner = event.getActioner();
                return actioner != null && users.contains(actioner.getId());
            });
        }

        // Add from/to filters (depends on timestamp strings comparing the right way!
        if (from.isPresent()) {
            pipe = pipe.filter(event -> {
                String timestamp = event.getTimestamp();
                return from.get().compareTo(timestamp) >= 0;
            });
        }

        if (to.isPresent()) {
            pipe = pipe.filter(event -> {
                String timestamp = event.getTimestamp();
                return to.get().compareTo(timestamp) <= 0;
            });
        }

        return pipe;
    }

    private GremlinPipeline<SystemEvent, SystemEvent> getPersonalisedEvents(UserProfile asUser, Accessor accessor) {
        // Add optional filters for event type, item type, and asUser...
        GremlinPipeline<SystemEvent, SystemEvent> pipe = new GremlinPipeline<>(
                actionManager.getLatestGlobalEvents());

        // Add additional generic filters
        pipe = filterEvents(pipe);

        // Filter out those we're not watching, or are actioned
        // by users we're not following...

        if (showType.contains(ShowType.watched)) {
            // Set IDs to items this asUser is watching...
            final List<String> watching = Lists.newArrayList();
            for (Watchable item : asUser.getWatching()) {
                watching.add(item.getId());
            }

            pipe = pipe.filter(event -> {
                for (Accessible e : event.getSubjects()) {
                    if (watching.contains(e.getId())) {
                        return true;
                    }
                }
                return false;
            });
        }

        if (showType.contains(ShowType.followed)) {
            final List<String> following = Lists.newArrayList();
            for (UserProfile other : asUser.getFollowing()) {
                following.add(other.getId());
            }

            pipe = pipe.filter(event -> {
                Actioner actioner = event.getActioner();
                return actioner != null && following.contains(actioner.getId());
            });
        }

        return applyAclFilter(pipe, accessor);
    }

    @Override
    public EventsApi from(String from) {
        return new EventsApiImpl.Builder(this)
                .from(from).build();
    }

    @Override
    public EventsApi to(String to) {
        return new EventsApiImpl.Builder(this)
                .to(to).build();
    }

    @Override
    public EventsApi withIds(String... ids) {
        return new EventsApiImpl.Builder(this)
                .withIds(ids).build();
    }

    @Override
    public EventsApi withRange(int offset, int limit) {
        return new EventsApiImpl.Builder(this)
                .withRange(offset, limit).build();
    }

    @Override
    public EventsApi withUsers(String... users) {
        return new EventsApiImpl.Builder(this)
                .withUsers(users).build();
    }

    @Override
    public EventsApi withEntityClasses(EntityClass... entityTypes) {
        return new EventsApiImpl.Builder(this)
                .withEntityTypes(entityTypes).build();
    }

    @Override
    public EventsApi withEventTypes(EventTypes... eventTypes) {
        return new EventsApiImpl.Builder(this)
                .withEventTypes(eventTypes).build();
    }

    @Override
    public EventsApi withShowType(ShowType... type) {
        return new EventsApiImpl.Builder(this)
                .withShowType(type).build();
    }

    @Override
    public EventsApi withAggregation(Aggregation aggregation) {
        return new EventsApiImpl.Builder(this)
                .withAggregation(aggregation).build();
    }

    private GremlinPipeline<SystemEvent, List<SystemEvent>> aggregateFilter(GremlinPipeline<SystemEvent, SystemEvent> pipeline) {
        switch (aggregation) {
            case strict:
                return pipeline.add(new AggregatorPipe<>(strictAggregator));
            case user:
                return pipeline.add(new AggregatorPipe<>(userAggregator));
            default:
                // Default case: no aggregation, so simply wrap each event in a list
                return pipeline.transform(new PipeFunction<SystemEvent, List<SystemEvent>>() {
                    @Override
                    public List<SystemEvent> compute(SystemEvent event) {
                        return Lists.newArrayList(event);
                    }
                });
        }
    }

    private <E, S> GremlinPipeline<E, S> setPipelineRange(
            GremlinPipeline<E, S> filter) {
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

    private <T> List<T> getItems(Collection<String> itemIds, Class<T> cls) {
        List<T> items = Lists.newArrayList();
        for (String itemId : itemIds) {
            try {
                items.add(manager.getEntity(itemId, cls));
            } catch (ItemNotFound itemNotFound) {
                logger.warn("Invalid event filter item: " + itemId);
            }
        }
        return items;
    }
}
