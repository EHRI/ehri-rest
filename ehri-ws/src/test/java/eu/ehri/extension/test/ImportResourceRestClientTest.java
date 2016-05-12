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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.extension.GenericResource;
import eu.ehri.extension.ImportResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ead.IcaAtomEadHandler;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.IOHelpers;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static eu.ehri.extension.ImportResource.*;
import static eu.ehri.project.test.IOHelpers.createZipFromResources;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;


public class ImportResourceRestClientTest extends AbstractRestClientTest {
    // NB: This file is *copied* into the extension test resources because
    // I can't figure out how to refer to resources in another module...
    protected static final String SINGLE_EAD = "ead.xml";
    protected static final String HIERARCHICAL_EAD = "hierarchical-ead.xml";

    @Test
    public void testImportSkos() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = ClassLoader.getSystemResourceAsStream("simple.n3");

        URI uri = getImportUrl("skos", "cvoc1", "Testing SKOS", true)
                .queryParam(FORMAT_PARAM, "Turtle")
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .entity(payloadStream)
                .post(ClientResponse.class);

        assertStatus(ClientResponse.Status.OK, response);
        ImportLog log = response.getEntity(ImportLog.class);
        assertEquals(1, log.getCreated());
        assertEquals(0, log.getUpdated());
        assertEquals(0, log.getUnchanged());
    }

    @Test
    public void testImportEadViaLocalPaths() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getPayloadStream(SINGLE_EAD);

        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", logText, false)
                .queryParam(HANDLER_PARAM, IcaAtomEadHandler.class.getName())
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(payloadStream)
                .post(ClientResponse.class);

        ImportLog log = response.getEntity(ImportLog.class);
        assertEquals(1, log.getCreated());
        assertEquals(0, log.getUpdated());
        assertEquals(0, log.getUnchanged());
        assertEquals(logText, log.getLogMessage().orNull());
    }

    @Test
    public void testImportSingleEad() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream(SINGLE_EAD);
        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", logText, false)
                .queryParam(HANDLER_PARAM, IcaAtomEadHandler.class.getName())
                .build();
        ImportLog log = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.TEXT_XML_TYPE)
                .entity(payloadStream)
                .post(ImportLog.class);

        assertEquals(1, log.getCreated());
        assertEquals(0, log.getUpdated());
        assertEquals(0, log.getUnchanged());
        assertEquals(logText, log.getLogMessage().orNull());
    }

    @Test
    public void testImportSingleEadWithModeViolation() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream(SINGLE_EAD);
        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", logText, false)
                .queryParam(HANDLER_PARAM, IcaAtomEadHandler.class.getName())
                .build();
        ImportLog log = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.TEXT_XML_TYPE)
                .entity(payloadStream)
                .post(ImportLog.class);

        assertEquals(1, log.getCreated());

        // Edit the item so subsequent EAD ingests will attempt to
        // change it.
        Bundle update = getEntity(Entities.DOCUMENTARY_UNIT, "nl-r1-test_doc", getAdminUserProfileId())
                .withDataValue("foo", "bar");
        URI uri2 = entityUri(Entities.DOCUMENTARY_UNIT, update.getId());
        jsonCallAs(getAdminUserProfileId(), uri2).put(update.toJson());

        ClientResponse response2 = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.TEXT_XML_TYPE)
                .entity(getClass()
                        .getClassLoader().getResourceAsStream(SINGLE_EAD))
                .post(ClientResponse.class);
        assertStatus(ClientResponse.Status.BAD_REQUEST, response2);
        assertThat(response2.getEntity(String.class),
                containsString("nl-r1-test_doc"));

        URI uri3 = getImportUrl("ead", "r1", logText, false)
                .queryParam(HANDLER_PARAM, IcaAtomEadHandler.class.getName())
                .queryParam(ALLOW_UPDATES_PARAM, "true")
                .build();
        ImportLog log2 = callAs(getAdminUserProfileId(), uri3)
                .type(MediaType.TEXT_XML_TYPE)
                .entity(getClass()
                        .getClassLoader().getResourceAsStream(SINGLE_EAD))
                .post(ImportLog.class);
        assertEquals(1, log2.getUpdated());
    }

    @Test
    public void testImportEadWithNonExistentClass() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getPayloadStream(SINGLE_EAD);

        URI uri = getImportUrl("ead", "r1", "Test", false)
                .queryParam(HANDLER_PARAM, "IDontExist") // oops
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(payloadStream)
                .post(ClientResponse.class);

        assertStatus(ClientResponse.Status.BAD_REQUEST, response);
        String output = response.getEntity(String.class);
        System.out.println(output);
        JsonNode rootNode = jsonMapper.readTree(output);
        assertTrue("Has correct error messages", rootNode.path("details").toString()
                .contains("Class not found"));
    }

    @Test
    public void testImportEadWithBadClass() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getPayloadStream(SINGLE_EAD);

        URI uri = getImportUrl("ead", "r1", "Test", false)
                .queryParam(HANDLER_PARAM, "java.lang.String") // oops
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(payloadStream)
                .post(ClientResponse.class);

        assertStatus(ClientResponse.Status.BAD_REQUEST, response);
        String output = response.getEntity(String.class);
        System.out.println(output);
        JsonNode rootNode = jsonMapper.readTree(output);
        assertTrue("Has correct error messages", rootNode.path("details").toString()
                .contains("not an instance of"));
    }

    @Test
    public void testImportEadWithFileLogMessage() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getPayloadStream(SINGLE_EAD);

        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", getTestLogFilePath(logText), false)
                .queryParam(HANDLER_PARAM, IcaAtomEadHandler.class.getName())
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(payloadStream)
                .post(ClientResponse.class);

        ImportLog log = response.getEntity(ImportLog.class);
        assertEquals(1, log.getCreated());
        assertEquals(0, log.getUpdated());
        assertEquals(0, log.getUnchanged());
        assertEquals(logText, log.getLogMessage().orNull());
    }

    @Test
    public void testImportEadWithMultipleFilesInZip() throws Exception {
        File temp = File.createTempFile("test-zip", ".zip");
        temp.deleteOnExit();
        createZipFromResources(temp, SINGLE_EAD, HIERARCHICAL_EAD);

        // Get the path of an EAD file
        InputStream payloadStream = new FileInputStream(temp);

        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", getTestLogFilePath(logText), false)
                .queryParam(HANDLER_PARAM, IcaAtomEadHandler.class.getName())
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .entity(payloadStream)
                .post(ClientResponse.class);

        ImportLog log = response.getEntity(ImportLog.class);
        assertEquals(6, log.getCreated());
        assertEquals(0, log.getUpdated());
        assertEquals(0, log.getUnchanged());
        assertEquals(logText, log.getLogMessage().orNull());
    }

    @Test
    public void testImportEadWithMultipleFilesInGZipTar() throws Exception {
        File temp = File.createTempFile("test-tar", ".tar");
        temp.deleteOnExit();
        File gzip = File.createTempFile("test-gzip", ".gz");
        createZipFromResources(temp, SINGLE_EAD, HIERARCHICAL_EAD);
        IOHelpers.gzipFile(temp, gzip);

        // Get the path of an EAD file
        InputStream payloadStream = new FileInputStream(gzip);

        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", getTestLogFilePath(logText), false)
                .queryParam(HANDLER_PARAM, IcaAtomEadHandler.class.getName())
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .entity(payloadStream)
                .post(ClientResponse.class);

        ImportLog log = response.getEntity(ImportLog.class);
        assertEquals(6, log.getCreated());
        assertEquals(0, log.getUpdated());
        assertEquals(0, log.getUnchanged());
        assertEquals(logText, log.getLogMessage().orNull());
    }

    @Test
    public void testImportEag() throws Exception {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("eag.xml");
        String logText = "Testing import";
        URI uri = getImportUrl("eag", "nl", logText, false)
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.TEXT_XML_TYPE)
                .entity(payloadStream)
                .post(ClientResponse.class);

        ImportLog log = response.getEntity(ImportLog.class);
        assertEquals(1, log.getCreated());
        assertEquals(0, log.getUpdated());
        assertEquals(0, log.getUnchanged());
        assertEquals(logText, log.getLogMessage().orNull());
    }

    @Test
    public void testImportEac() throws Exception {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("eac.xml");
        String logText = "Testing import";
        URI uri = getImportUrl("eac", "auths", logText, false)
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .type(MediaType.TEXT_XML_TYPE)
                .entity(payloadStream)
                .post(ClientResponse.class);

        ImportLog log = response.getEntity(ImportLog.class);
        assertEquals(1, log.getCreated());
        assertEquals(0, log.getUpdated());
        assertEquals(0, log.getUnchanged());
        assertEquals(logText, log.getLogMessage().orNull());
    }

    @Test
    public void testBatchUpdate() throws Exception {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-patch-test.json");
        System.out.println(payloadStream);
        String logText = "Testing patch update";
        URI jsonUri = ehriUriBuilder(ImportResource.ENDPOINT, "batch")
                .queryParam(LOG_PARAM, logText).build();
        ClientResponse response = callAs(getAdminUserProfileId(), jsonUri)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(payloadStream)
                .put(ClientResponse.class);

        ImportLog log = response.getEntity(ImportLog.class);
        assertEquals(0, log.getCreated());
        assertEquals(1, log.getUpdated());
        assertEquals(1, log.getUnchanged());
        assertEquals(logText, log.getLogMessage().orNull());
    }

    @Test
    public void testBatchUpdateToUnsetValues() throws Exception {
        Bundle before = getEntity(Entities.REPOSITORY, "r1",
                getAdminUserProfileId());
        assertEquals("Repository 1", before.getDataValue("name"));
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-patch-test2.json");
        String logText = "Testing patch to unset values";
        URI jsonUri = ehriUriBuilder(ImportResource.ENDPOINT, "batch")
                .queryParam(LOG_PARAM, logText)
                .queryParam(SCOPE_PARAM, "nl").build();
        ClientResponse response = callAs(getAdminUserProfileId(), jsonUri)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(payloadStream)
                .put(ClientResponse.class);

        String output = response.getEntity(String.class);
        System.out.println(output);
        assertStatus(ClientResponse.Status.OK, response);

        Bundle after = getEntity(Entities.REPOSITORY, "r1",
                getAdminUserProfileId());
        assertNull(after.getDataValue("name"));
    }

    @Test
    public void testBatchDelete() throws Exception {
        String user = getAdminUserProfileId();
        assertTrue(checkExists("a2", user));
        String logText = "Testing patch delete";
        URI jsonUri = ehriUriBuilder(ImportResource.ENDPOINT, "batch")
                .queryParam(LOG_PARAM, logText)
                .queryParam(ID_PARAM, "a2")
                .build();
        ClientResponse response = callAs(user, jsonUri)
                .delete(ClientResponse.class);
        assertStatus(ClientResponse.Status.NO_CONTENT, response);
        assertFalse(checkExists("a2", user));
    }

    private boolean checkExists(String id, String userId) {
        URI uri = ehriUriBuilder(GenericResource.ENDPOINT, id).build();
        return callAs(userId, uri).get(ClientResponse.class).getStatus()
                == ClientResponse.Status.OK.getStatusCode();
    }

    private UriBuilder getImportUrl(String endPoint, String scopeId, String log, boolean tolerant) {
        return ehriUriBuilder(ImportResource.ENDPOINT, endPoint)
                .queryParam(LOG_PARAM, log)
                .queryParam(SCOPE_PARAM, scopeId)
                .queryParam(TOLERANT_PARAM, String.valueOf(tolerant));
    }

    private InputStream getPayloadStream(String... resources)
            throws URISyntaxException, UnsupportedEncodingException {
        List<String> paths = Lists.newArrayList();
        for (String resourceName : resources) {
            URL resource = Resources.getResource(resourceName);
            paths.add(new File(resource.toURI()).getAbsolutePath());
        }
        String payloadText = Joiner.on("\n").join(paths) + "\n";
        return new ByteArrayInputStream(
                payloadText.getBytes("UTF-8"));
    }

    private String getTestLogFilePath(String text) throws IOException {
        File temp = File.createTempFile("test-log", ".tmp");
        temp.deleteOnExit();
        FileUtils.writeStringToFile(temp, text);
        return temp.getAbsolutePath();
    }
}
