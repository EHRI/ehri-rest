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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import eu.ehri.extension.base.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;

/**
 * Base class for testing the REST interface on a 'embedded' neo4j server.
 */
public class AbstractRestClientTest extends RunningServerTest {

    protected static final Client client = Client.create();

    protected static final ObjectMapper jsonMapper = new ObjectMapper();

    protected static final Pattern paginationPattern = Pattern.compile("offset=(-?\\d+); limit=(-?\\d+); total=(-?\\d+)");

    // Admin user prefix - depends on fixture data
    final static private String adminUserProfileId = "mike";

    // Regular user
    final static private String regularUserProfileId = "reto";

    protected String getAdminUserProfileId() {
        return adminUserProfileId;
    }

    protected String getRegularUserProfileId() {
        return regularUserProfileId;
    }

    /**
     * Tests if we have an admin user, we need that user for doing all the other
     * tests
     */
    @Test
    public void testAdminGetUserProfile() throws Exception {
        // get the admin user profile
        WebResource resource = client.resource(
                entityUri(Entities.USER_PROFILE, getAdminUserProfileId()));
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertStatus(OK, response);
    }

    /**
     * Helpers **
     */

    protected List<Map<String, Object>> getItemList(URI uri, String userId) throws Exception {
        return getItemList(uri, userId, new MultivaluedMapImpl());
    }

    /**
     * Get a list of items at some url, as the given user.
     */
    protected List<Map<String, Object>> getItemList(URI uri, String userId,
            MultivaluedMap<String, String> params) throws Exception {
        TypeReference<LinkedList<HashMap<String, Object>>> typeRef = new TypeReference<LinkedList<HashMap<String, Object>>>() {
        };
        return jsonMapper.readValue(getJson(uri, userId, params), typeRef);
    }

    protected List<List<Map<String, Object>>> getItemListOfLists(URI uri, String userId) throws Exception {
        return getItemListOfLists(uri, userId, new MultivaluedMapImpl());
    }

    /**
     * Get a list of items at some relativeUrl, as the given user.
     */
    protected List<List<Map<String, Object>>> getItemListOfLists(URI uri, String userId,
            MultivaluedMap<String, String> params) throws Exception {
        TypeReference<LinkedList<LinkedList<HashMap<String, Object>>>> typeRef = new
                TypeReference<LinkedList<LinkedList<HashMap<String, Object>>>>() {
                };
        return jsonMapper.readValue(getJson(uri, userId, params), typeRef);
    }

    /**
     * Function for fetching a list of entities with the given EntityType
     */
    protected List<Map<String, Object>> getEntityList(String entityType, String userId)
            throws Exception {
        return getEntityList(entityUri(entityType), userId, new MultivaluedMapImpl());
    }

    /**
     * Function for fetching a list of entities with the given EntityType,
     * and some additional parameters.
     */
    protected List<Map<String, Object>> getEntityList(URI uri,
            String userId, MultivaluedMap<String, String> params) throws Exception {
        return getItemList(uri, userId, params);
    }

    protected Integer getPaginationTotal(ClientResponse response) {
        MultivaluedMap<String,String> headers = response.getHeaders();
        String range = headers.getFirst("Content-Range");
        if (range != null && range.matches(paginationPattern.pattern())) {
            Matcher matcher = paginationPattern.matcher(range);
            return matcher.find() ? Integer.valueOf(matcher.group(3)) : null;
        }
        return null;
    }

    protected Long getEntityCount(String entityType, String userId) {
        WebResource resource = client.resource(entityUri(entityType));
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, userId)
                .head();
        return Long.valueOf(getPaginationTotal(response));
    }

    protected UriBuilder ehriUriBuilder(String... segments) {
        UriBuilder builder = UriBuilder.fromPath(getExtensionEntryPointUri());
        for (String segment : segments) {
            builder = builder.segment(segment);
        }
        return builder;
    }

    protected UriBuilder entityUriBuilder(String entityType, String... segments) {
        List<String> segs = Lists.newArrayList(
                AbstractRestResource.RESOURCE_ENDPOINT_PREFIX,
                entityType);
        segs.addAll(Lists.newArrayList(segments));
        return ehriUriBuilder(segs.toArray(new String[segs.size()]));
    }

    protected URI entityUri(String entityType, String... segments) {
        List<String> segs = Lists.newArrayList(
                AbstractRestResource.RESOURCE_ENDPOINT_PREFIX,
                entityType);
        segs.addAll(Lists.newArrayList(segments));
        return ehriUriBuilder(segs.toArray(new String[segs.size()])).build();
    }

    protected URI ehriUri(String... segments) {
        return ehriUriBuilder(segments).build();
    }

    protected WebResource.Builder jsonCallAs(String user, URI uri) {
        return callAs(user, uri)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON);
    }

    protected WebResource.Builder jsonCallAs(String user, String... segments) {
        return callAs(user, segments)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON);
    }

    protected WebResource.Builder callAs(String user, URI uri) {
        return client.resource(uri)
                .header(AbstractRestResource.AUTH_HEADER_NAME, user);
    }

    protected WebResource.Builder callAs(String user, String... segments) {
        return callAs(user, ehriUriBuilder(segments).build());
    }

    public void assertStatus(ClientResponse.Status status, ClientResponse response) {
        org.junit.Assert.assertEquals(status.getStatusCode(), response.getStatus());
    }

    public void assertValidJsonData(ClientResponse response) {
        try {
            Bundle.fromString(response.getEntity(String.class));
        } catch (DeserializationError deserializationError) {
            throw new RuntimeException(deserializationError);
        }
    }

    protected String readResourceFileAsString(String resourceName)
            throws java.io.IOException {
        URL url = Resources.getResource(resourceName);
        return Resources.toString(url, Charsets.UTF_8);
    }

    private String getJson(URI uri, String userId, MultivaluedMap<String, String> params) {
        WebResource resource = client.resource(uri).queryParams(params);
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME, userId)
                .get(ClientResponse.class);
        return response.getEntity(String.class);
    }
}