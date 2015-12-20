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

package eu.ehri.extension.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.extension.SystemEventResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.views.EventViews;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static eu.ehri.extension.AbstractRestResource.ID_PARAM;
import static eu.ehri.extension.UserProfileResource.FOLLOWING;
import static eu.ehri.extension.UserProfileResource.WATCHING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SystemEventRestClientTest extends BaseRestClientTest {

    static final String COUNTRY_CODE = "nl";

    private String jsonAgentTestString = "{\"type\": \"Repository\", \"data\":{\"identifier\": \"jmp\"}}";

    @Test
    public void testListActions() throws Exception {
        // Create a new agent. We're going to test that this creates
        // a corresponding action.
        String url = "/" + Entities.SYSTEM_EVENT;
        List<List<Map<String, Object>>> actionsBefore = getItemListOfLists(
                url, getAdminUserProfileId());

        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.COUNTRY, COUNTRY_CODE, Entities.REPOSITORY))
                .entity(jsonAgentTestString)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);

        List<List<Map<String, Object>>> actionsAfter = getItemListOfLists(
                url, getAdminUserProfileId());
        System.out.println(actionsAfter);

        // Having created a new Repository, we should have at least one Event.
        assertEquals(actionsBefore.size() + 1, actionsAfter.size());
    }

    @Test
    public void testListActionsWithFilter() throws Exception {
        // Create a new agent. We're going to test that this creates
        // a corresponding action.

        jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.COUNTRY, COUNTRY_CODE, Entities.REPOSITORY))
                .entity(jsonAgentTestString)
                .post(ClientResponse.class);

        // Add a good user filter...
        MultivaluedMap<String, String> goodFilters = new MultivaluedMapImpl();
        goodFilters.add(SystemEventResource.USER_PARAM, getAdminUserProfileId());

        // Add a useless filter that should remove all results
        MultivaluedMap<String, String> badFilters = new MultivaluedMapImpl();
        badFilters.add(SystemEventResource.USER_PARAM, "nobody");

        String url = "/" + Entities.SYSTEM_EVENT;
        List<List<Map<String, Object>>> goodFiltered = getItemListOfLists(
                url, getAdminUserProfileId(), goodFilters);

        List<List<Map<String, Object>>> badFiltered = getItemListOfLists(
                url, getAdminUserProfileId(), badFilters);

        assertTrue(goodFiltered.size() > 0);
        assertEquals(0, badFiltered.size());
    }

    @Test
    public void testGetActionsForItem() throws Exception {

        // Create an item
        client.resource(
                ehriUri(Entities.COUNTRY, COUNTRY_CODE, Entities.REPOSITORY));
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.COUNTRY, COUNTRY_CODE, Entities.REPOSITORY))
                .entity(jsonAgentTestString)
                .post(ClientResponse.class);

        // Get the id
        String url = response.getLocation().toString();
        String id = url.substring(url.lastIndexOf("/") + 1);

        response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.SYSTEM_EVENT, "for", id))
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testPersonalisedEventList() throws Exception {

        // Create an event by updating an item...
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.REPOSITORY, "r1"))
                .entity(jsonAgentTestString)
                .header(AbstractRestResource.LOG_MESSAGE_HEADER_NAME, "Testing update")
                .put(ClientResponse.class);
        assertStatus(OK, response);

        // At present, the personalised event stream for the validUser user should
        // contain all the regular events.
        String user = getRegularUserProfileId();
        String personalisedEventUrl = "/" + Entities.SYSTEM_EVENT + "/aggregateForUser/" + user;
        List<List<Map<String, Object>>> events = getItemListOfLists(personalisedEventUrl, user);
        assertEquals(1, events.size());

        // Now only fetch events related to items we're watching - this list
        // should currently be empty...
        String personalisedEventUrlWatched = personalisedEventUrl + "?" + SystemEventResource.SHOW_PARAM + "=" + EventViews.ShowType.watched;
        events = getItemListOfLists(personalisedEventUrlWatched, user);
        assertEquals(0, events.size());

        // Now start watching item r1.
        URI watchUrl = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user)
                .segment(WATCHING)
                .queryParam(ID_PARAM, "r1").build();

        jsonCallAs(user, watchUrl).post(ClientResponse.class);

        // Now our event list should contain one item, the update
        // we did initially.
        events = getItemListOfLists(personalisedEventUrlWatched, user);
        assertEquals(1, events.size());

        // Stop watching item r1, which should empty the list
        jsonCallAs(user, watchUrl).delete(ClientResponse.class);

        events = getItemListOfLists(personalisedEventUrlWatched, user);
        assertEquals(0, events.size());

        // Only get events for people we follow, excluding those
        // for items we watch...
        String personalisedEventUrlFollowed = personalisedEventUrl + "?" + SystemEventResource.SHOW_PARAM
                + "=" + EventViews.ShowType.followed;
        events = getItemListOfLists(personalisedEventUrlFollowed, user);
        assertEquals(0, events.size());

        // Now follow the other user...
        URI followUrl = UriBuilder.fromPath(getExtensionEntryPointUri())
                .segment(Entities.USER_PROFILE)
                .segment(user)
                .segment(FOLLOWING)
                .queryParam(ID_PARAM, getAdminUserProfileId()).build();
        jsonCallAs(user, followUrl).post(ClientResponse.class);

        // We should get the event again...
        events = getItemListOfLists(personalisedEventUrlFollowed, user);
        assertEquals(1, events.size());
    }
}
