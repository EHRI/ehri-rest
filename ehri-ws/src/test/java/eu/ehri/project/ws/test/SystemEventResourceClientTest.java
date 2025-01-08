/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.ws.test;

import eu.ehri.project.api.EventsApi;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.ws.GenericResource;
import eu.ehri.project.ws.base.AbstractAccessibleResource;
import eu.ehri.project.ws.base.AbstractResource;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SystemEventResourceClientTest extends AbstractResourceClientTest {

    private static final String COUNTRY_CODE = "nl";

    private static final String jsonAgentTestString = "{\"type\": \"Repository\", \"data\":{\"identifier\": \"jmp\"}}";

    @Test
    public void testListActions() throws Exception {
        // Create a new agent. We're going to test that this creates
        // a corresponding action.
        URI uri = entityUri(Entities.SYSTEM_EVENT);
        List<List<Bundle>> actionsBefore = getItemListOfLists(
                uri, getAdminUserProfileId());

        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.COUNTRY, COUNTRY_CODE))
                .post(Entity.json(jsonAgentTestString), Response.class);

        assertStatus(CREATED, response);

        List<List<Bundle>> actionsAfter = getItemListOfLists(
                uri, getAdminUserProfileId());
        System.out.println(actionsAfter);

        // Having created a new Repository, we should have at least one Event.
        assertEquals(actionsBefore.size() + 1, actionsAfter.size());
    }

    @Test
    public void testListActionsWithFilter() throws Exception {
        // Create a new agent. We're going to test that this creates
        // a corresponding action.

        jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.COUNTRY, COUNTRY_CODE))
                .post(Entity.json(jsonAgentTestString), Response.class);

        // Add a good user filter...
        MultivaluedMap<String, String> goodFilters = new MultivaluedHashMap<>();
        goodFilters.add(AbstractAccessibleResource.USER_PARAM, getAdminUserProfileId());

        // Add a useless filter that should remove all results
        MultivaluedMap<String, String> badFilters = new MultivaluedHashMap<>();
        badFilters.add(AbstractAccessibleResource.USER_PARAM, "nobody");

        URI url = entityUri(Entities.SYSTEM_EVENT);
        List<List<Bundle>> goodFiltered = getItemListOfLists(
                url, getAdminUserProfileId(), goodFilters);

        List<List<Bundle>> badFiltered = getItemListOfLists(
                url, getAdminUserProfileId(), badFilters);

        assertFalse(goodFiltered.isEmpty());
        assertEquals(0, badFiltered.size());
    }

    @Test
    public void testGetActionsForItem() throws Exception {

        // Create an item
        URI uri = entityUri(Entities.COUNTRY, COUNTRY_CODE);
        Response response = jsonCallAs(getAdminUserProfileId(), uri)
                .post(Entity.json(jsonAgentTestString), Response.class);

        // Get the id
        String url = response.getLocation().toString();
        String id = url.substring(url.lastIndexOf("/") + 1);

        response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(GenericResource.ENDPOINT, id, "events"))
                .get(Response.class);
        assertStatus(OK, response);
    }

    @Test
    public void testPersonalisedEventList() throws Exception {

        // Create an event by updating an item...
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.REPOSITORY, "r1"))
                .header(AbstractResource.LOG_MESSAGE_HEADER_NAME, "Testing update")
                .put(Entity.json(jsonAgentTestString), Response.class);
        assertStatus(OK, response);

        // At present, the personalised event stream for the validUser user should
        // contain all the regular events.
        String user = getRegularUserProfileId();
        URI personalisedEventUrl = entityUri(Entities.USER_PROFILE, user, "events");
        List<List<Bundle>> events = getItemListOfLists(personalisedEventUrl, user);
        assertEquals(1, events.size());

        // Now only fetch events related to items we're watching - this list
        // should currently be empty...
        URI personalisedEventUrlWatched = entityUriBuilder(
                Entities.USER_PROFILE, user, "events")
                .queryParam(AbstractAccessibleResource.SHOW_PARAM, EventsApi.ShowType.watched).build();
        events = getItemListOfLists(personalisedEventUrlWatched, user);
        assertEquals(0, events.size());

        // Now start watching item r1.
        URI watchUrl = entityUriBuilder(Entities.USER_PROFILE, user, "watching")
                .queryParam(AbstractResource.ID_PARAM, "r1").build();

        jsonCallAs(user, watchUrl).post(Entity.json(""), Response.class);

        // Now our event list should contain one item, the update
        // we did initially.
        events = getItemListOfLists(personalisedEventUrlWatched, user);
        assertEquals(1, events.size());

        // Stop watching item r1, which should empty the list
        jsonCallAs(user, watchUrl).delete(Response.class);

        events = getItemListOfLists(personalisedEventUrlWatched, user);
        assertEquals(0, events.size());

        // Only get events for people we follow, excluding those
        // for items we watch...
        URI personalisedEventUrlFollowed = entityUriBuilder(
                Entities.USER_PROFILE, user, "events")
                .queryParam(AbstractAccessibleResource.SHOW_PARAM, EventsApi.ShowType.followed).build();
        events = getItemListOfLists(personalisedEventUrlFollowed, user);
        assertEquals(0, events.size());

        // Now follow the other user...
        URI followUrl = entityUriBuilder(Entities.USER_PROFILE, user, "following")
                .queryParam(AbstractResource.ID_PARAM, getAdminUserProfileId()).build();
        jsonCallAs(user, followUrl).post(Entity.json(""), Response.class);

        // We should get the event again...
        events = getItemListOfLists(personalisedEventUrlFollowed, user);
        assertEquals(1, events.size());
    }
}
