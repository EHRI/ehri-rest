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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import eu.ehri.project.ws.base.AbstractResource;
import eu.ehri.project.ws.providers.*;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDeserializer;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Base class for testing the web service interface on a 'embedded' neo4j server.
 */
public class AbstractResourceClientTest extends RunningServerTest {

    protected Client client;

    AbstractResourceClientTest(Class<?> ... additionalProviders) {
        ClientConfig config = new ClientConfig();
        Lists.<Class<?>>newArrayList(
                GlobalPermissionSetProvider.class,
                TableProvider.class,
                BundleProvider.class,
                ImportLogProvider.class,
                SyncLogProvider.class
        ).forEach(config::register);
        Lists.newArrayList(additionalProviders)
                .forEach(config::register);

        client = ClientBuilder.newClient(config);
    }

    protected List<ZipEntry> readZip(InputStream stream) throws IOException {
        File tmp = File.createTempFile("test", ".zip");
        tmp.deleteOnExit();
        Files.copy(stream, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        final List<ZipEntry> entries = Lists.newArrayList();
        try (InputStream fis = Files.newInputStream(tmp.toPath());
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                entries.add(zipEntry);
            }
        }
        return entries;
    }

    protected static final ObjectMapper jsonMapper;

    static {
        SimpleModule bundleModule = new SimpleModule();
        bundleModule.addDeserializer(Bundle.class, new BundleDeserializer());
        jsonMapper = JsonMapper
                .builder()
                .addModule(bundleModule)
                .build();
    }

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
     * Helpers **
     */

    protected List<Bundle> getItemList(URI uri, String userId) throws Exception {
        return getItemList(uri, userId, new MultivaluedHashMap<>());
    }

    protected List<Bundle> decodeList(String data) throws Exception {
        TypeReference<LinkedList<Bundle>> typeRef = new TypeReference<LinkedList<Bundle>>() {
        };
        return jsonMapper.readValue(data, typeRef);
    }

    /**
     * Get a list of items at some url, as the given user.
     */
    protected List<Bundle> getItemList(URI uri, String userId, MultivaluedMap<String, String> params) throws Exception {
        return decodeList(getJson(uri, userId, params));
    }

    protected List<List<Bundle>> getItemListOfLists(URI uri, String userId) throws Exception {
        return getItemListOfLists(uri, userId, new MultivaluedHashMap<>());
    }

    /**
     * Get a list of items at some relativeUrl, as the given user.
     */
    protected List<List<Bundle>> getItemListOfLists(URI uri, String userId, MultivaluedMap<String, String> params) throws Exception {
        TypeReference<List<List<Bundle>>> typeRef = new TypeReference<>() {
        };
        return jsonMapper.readValue(getJson(uri, userId, params), typeRef);
    }

    /**
     * Function for fetching a list of entities with the given EntityType
     */
    protected List<Bundle> getEntityList(String entityType, String userId)
            throws Exception {
        return getEntityList(entityUri(entityType), userId, new MultivaluedHashMap<>());
    }

    /**
     * Get an item as a bundle object.
     *
     * @param type the item's type
     * @param id   the items ID
     * @return a bundle
     */
    protected Bundle getEntity(String type, String id, String userId) throws Exception {
        return jsonMapper.readValue(getJson(
                entityUri(type, id), userId, new MultivaluedHashMap<>()), Bundle.class);
    }

    /**
     * Function for fetching a list of entities with the given EntityType,
     * and some additional parameters.
     */
    protected List<Bundle> getEntityList(URI uri,
            String userId, MultivaluedMap<String, String> params) throws Exception {
        return getItemList(uri, userId, params);
    }

    protected int getPaginationTotal(Response response) {
        MultivaluedMap<String, Object> headers = response.getHeaders();
        String range = (String)headers.getFirst(AbstractResource.RANGE_HEADER_NAME);
        if (range != null && range.matches(paginationPattern.pattern())) {
            Matcher matcher = paginationPattern.matcher(range);
            return matcher.find() ? Integer.parseInt(matcher.group(3)) : -1;
        }
        return -1;
    }

    protected long getEntityCount(String entityType, String userId) {
        WebTarget target = client.target(entityUri(entityType));
        Response response = target.request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME, userId)
                .head();
        return getPaginationTotal(response);
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
                AbstractResource.RESOURCE_ENDPOINT_PREFIX,
                entityType);
        segs.addAll(Lists.newArrayList(segments));
        return ehriUriBuilder(segs.toArray(new String[0]));
    }

    protected URI entityUri(String entityType, String... segments) {
        List<String> segs = Lists.newArrayList(
                AbstractResource.RESOURCE_ENDPOINT_PREFIX,
                entityType);
        segs.addAll(Lists.newArrayList(segments));
        return ehriUriBuilder(segs.toArray(new String[0])).build();
    }

    protected URI ehriUri(String... segments) {
        return ehriUriBuilder(segments).build();
    }

    protected Invocation.Builder jsonCallAs(String user, URI uri) {
        return callAs(user, uri)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected Invocation.Builder jsonCallAs(String user, String... segments) {
        return callAs(user, segments)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected Invocation.Builder callAs(String user, URI uri) {
        return client.target(uri)
                .request()
                .header(AbstractResource.AUTH_HEADER_NAME, user);
    }

    protected Invocation.Builder callAs(String user, String... segments) {
        return callAs(user, ehriUriBuilder(segments).build());
    }

    protected void assertStatus(Status status, Response response) {
        org.junit.Assert.assertEquals(status.getStatusCode(), response.getStatus());
    }

    protected void assertValidJsonData(Response response) {
        try {
            Bundle.fromString(response.readEntity(String.class));
        } catch (DeserializationError deserializationError) {
            throw new RuntimeException(deserializationError);
        }
    }

    protected String readResourceFileAsString(String resourceName)
            throws java.io.IOException {
        URL url = Resources.getResource(resourceName);
        return Resources.toString(url, Charsets.UTF_8);
    }


    protected final Comparator<Bundle> bundleComparator = Comparator.comparing(Bundle::getId);

    private String getJson(URI uri, String userId, MultivaluedMap<String, String> params) {
        WebTarget target = client.target(uri);
        for (String key : params.keySet()) {
            target = target.queryParam(key, params.getFirst(key));
        }
        return target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME, userId)
                .get(String.class);
    }
}
