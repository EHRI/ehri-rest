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

package eu.ehri.extension;

import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.project.api.EventsApi;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.events.SystemEvent;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides a web service interface for the Event model. Note: Event instances
 * are created by the system, so we do not have create/update/delete methods
 * here.
 * <p>
 * The following query parameters apply to all actions in this
 * resource to apply filtering to the event streams.
 * <p>
 * <dl>
 * <dt>eventTypes</dt><dd>Filter events by type</dd>
 * <dt>itemTypes</dt><dd>Filter events based on the item type of their subjects</dd>
 * <dt>itemIds</dt><dd>Filter events pertaining to specific item IDs</dd>
 * <dt>users</dt><dd>Filter events based on the user IDs they involve</dd>
 * <dt>from</dt><dd>Exclude events prior to this date (ISO 8601 format)</dd>
 * <dt>to</dt><dd>Exclude events after this date (ISO 8601 format)</dd>
 * </dl>
 * <p>
 * Additionally the aggregate* end-points accept an <code>aggregation</code>
 * parameter that groups sequential events according to one of two different
 * strategies:
 * <dl>
 * <dt>user</dt>
 * <dd>Groups sequential events of all types that are initiated by the same actioner</dd>
 * <dt>strict</dt>
 * <dd>
 * Groups sequential events that:
 * <ul>
 * <li>have the same type</li>
 * <li>have the same actioner</li>
 * <li>have the same subjects</li>
 * <li>have the same scope</li>
 * <li>have the same log message</li>
 * </ul>
 * </dd>
 * </dl>
 * <p>
 * Additionally, aggregation can be disabled by using <code>aggregation=off</code>.
 * <p>
 * Standard paging parameters apply to all end-points.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.SYSTEM_EVENT)
public class SystemEventResource extends AbstractAccessibleResource<SystemEvent>
        implements GetResource {


    public SystemEventResource(@Context GraphDatabaseService database) {
        super(database, SystemEvent.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        return getItem(id);
    }

    /**
     * List aggregated global events. Standard list parameters for paging apply.
     *
     * @param aggregation The manner in which to aggregate the results, accepting
     *                    "user", "strict" or "off" (no aggregation). Default is
     *                    "user".
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(
            @QueryParam(AGGREGATION_PARAM) @DefaultValue("user") EventsApi.Aggregation aggregation) {
        try (final Tx tx = beginTx()) {
            EventsApi eventsApi = getEventsApi()
                    .withAggregation(aggregation);
            Response response = streamingListOfLists(eventsApi.aggregate());
            tx.success();
            return response;
        }
    }

    /**
     * Fetch a page of subjects for a given event.
     *
     * @param id the event id
     * @return a list of subject items
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/subjects")
    public Response pageSubjectsForEvent(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied {
        try (final Tx tx = beginTx()) {
            SystemEvent event = api().detail(id, cls);
            // Subjects are only serialized to depth 1 for efficiency...
            Response response = streamingPage(getQuery()
                            .page(event.getSubjects(), Accessible.class),
                    getSerializer().withDepth(1).withCache());
            tx.success();
            return response;
        }
    }
}
