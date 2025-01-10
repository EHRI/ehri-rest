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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import eu.ehri.project.ws.ImportResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ead.EadHandler;
import eu.ehri.project.importers.ead.SyncLog;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.IOHelpers;
import eu.ehri.project.utils.Table;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static eu.ehri.project.ws.ImportResource.*;
import static eu.ehri.project.test.IOHelpers.createZipFromResources;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ImportResourceClientTest extends AbstractResourceClientTest {
    // NB: This file is *copied* into the extension test resources because
    // I can't figure out how to refer to resources in another module...
    private static final String SINGLE_EAD = "ead.xml";
    private static final String HIERARCHICAL_EAD = "hierarchical-ead.xml";

    @Test
    public void testImportSkos() {
        // Get the path of an EAD file
        InputStream payloadStream = ClassLoader.getSystemResourceAsStream("simple.n3");

        URI uri = getImportUrl("skos", "cvoc1", "Testing SKOS", true)
                .queryParam(FORMAT_PARAM, "Turtle")
                .build();
        Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.json(payloadStream), Response.class);

        assertStatus(Response.Status.OK, response);
        ImportLog log = response.readEntity(ImportLog.class);
        assertEquals(1, log.getCreated());
        assertEquals(0, log.getUpdated());
        assertEquals(0, log.getUnchanged());
        assertThat(log.getEventId().orElse(null), notNullValue());
    }

    @Test
    public void testImportEadViaJsonUrlMap() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getJsonObjectPayloadStream(ImmutableMap.of("single-ead",
                Resources.getResource(SINGLE_EAD).toURI().toString()));

        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", logText, false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .build();

        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.json(payloadStream), Response.class)) {
            ImportLog log = response.readEntity(ImportLog.class);
            assertEquals(1, log.getCreated());
            assertEquals(0, log.getUpdated());
            assertEquals(0, log.getUnchanged());
            assertEquals(logText, log.getLogMessage().orElse(null));
            assertThat(log.getEventId().orElse(null), notNullValue());
        }
    }

    @Test
    public void testImportEadViaLocalPaths() throws Exception {
        // Get the path of an EAD file
        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", logText, false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .build();
        try (InputStream payloadStream = getResourcePathsPayloadStream(SINGLE_EAD);
                Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.text(payloadStream), Response.class)) {

            ImportLog log = response.readEntity(ImportLog.class);
            assertEquals(1, log.getCreated());
            assertEquals(0, log.getUpdated());
            assertEquals(0, log.getUnchanged());
            assertEquals(logText, log.getLogMessage().orElse(null));
            assertThat(log.getEventId().orElse(null), notNullValue());
        }
    }

    @Test
    public void testImportSingleEad() {
        // Get the path of an EAD file
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream(SINGLE_EAD);
        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", logText, false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(TAG_PARAM, SINGLE_EAD)
                .queryParam(COMMIT_PARAM, true)
                .build();
        ImportLog log = callAs(getAdminUserProfileId(), uri)
                .post(Entity.xml(payloadStream), ImportLog.class);

        assertEquals(1, log.getCreated());
        assertEquals(0, log.getUpdated());
        assertEquals(0, log.getUnchanged());
        assertTrue("Tag is not in log", log.getCreatedKeys().containsKey(SINGLE_EAD));
        assertEquals(logText, log.getLogMessage().orElse(null));
        assertThat(log.getEventId().orElse(null), notNullValue());
    }

    @Test
    public void testImportSingleEadWithValidationError() {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("invalid-ead.xml");
        URI uri = getImportUrl("ead", "r1", "Error test", false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(COMMIT_PARAM, true)
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.xml(payloadStream), Response.class)) {

            System.out.println(response.readEntity(String.class));
            assertStatus(Response.Status.BAD_REQUEST, response);
        }
    }

    @Test
    public void testImportEadWithValidationError() {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("anonymous-c-levels.xml");
        URI uri = getImportUrl("ead", "r1", "Error test", false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(COMMIT_PARAM, true)
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.xml(payloadStream), Response.class)) {

            System.out.println(response.readEntity(String.class));
            assertStatus(Response.Status.BAD_REQUEST, response);
        }
    }

    @Test
    public void testImportSingleEadWithValidationErrorInTolerantMode() {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("invalid-ead.xml");
        URI uri = getImportUrl("ead", "r1", "Error test", true)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(COMMIT_PARAM, true)
                .build();

        ImportLog log = callAs(getAdminUserProfileId(), uri)
                .post(Entity.xml(payloadStream), ImportLog.class);
        assertEquals(1, log.getErrored());
    }

    @Test
    public void testImportSingleEadWithModeViolation() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream(SINGLE_EAD);
        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", logText, false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(COMMIT_PARAM, true)
                .build();
        ImportLog log = callAs(getAdminUserProfileId(), uri)
                .post(Entity.xml(payloadStream), ImportLog.class);

        assertEquals(1, log.getCreated());

        // Edit the item so subsequent EAD ingests will attempt to
        // change it.
        Bundle update = getEntity(Entities.DOCUMENTARY_UNIT, "nl-r1-test_doc", getAdminUserProfileId())
                .withDataValue("foo", "bar");
        URI uri2 = entityUri(Entities.DOCUMENTARY_UNIT, update.getId());
        try (Response r = jsonCallAs(getAdminUserProfileId(), uri2).put(Entity.json(update.toJson()))) {
            assertStatus(Response.Status.OK, r);
        }

        final InputStream payloadStream2 = getClass()
                .getClassLoader().getResourceAsStream(SINGLE_EAD);

        try (Response response2 = callAs(getAdminUserProfileId(), uri)
                .post(Entity.xml(payloadStream2), Response.class)) {
            assertStatus(Response.Status.BAD_REQUEST, response2);
            assertThat(response2.readEntity(String.class),
                    containsString("nl-r1-test_doc"));

            URI uri3 = getImportUrl("ead", "r1", logText, false)
                    .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                    .queryParam(ALLOW_UPDATES_PARAM, "true")
                    .queryParam(COMMIT_PARAM, true)
                    .build();
            final InputStream payloadStream3 = getClass()
                    .getClassLoader().getResourceAsStream(SINGLE_EAD);
            ImportLog log2 = callAs(getAdminUserProfileId(), uri3)
                    .post(Entity.xml(payloadStream3), ImportLog.class);
            assertEquals(1, log2.getUpdated());
        }
    }

    @Test
    public void testImportEadWithNonExistentClass() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getResourcePathsPayloadStream(SINGLE_EAD);

        URI uri = getImportUrl("ead", "r1", "Test", false)
                .queryParam(HANDLER_PARAM, "IDontExist") // oops
                .queryParam(COMMIT_PARAM, true)
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.text(payloadStream), Response.class)) {

            assertStatus(Response.Status.BAD_REQUEST, response);
            String output = response.readEntity(String.class);
            JsonNode rootNode = jsonMapper.readTree(output);
            assertTrue("Has correct error messages", rootNode.path("details").toString()
                    .contains("Class not found"));
        }
    }

    @Test
    public void testImportEadWithBadClass() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getResourcePathsPayloadStream(SINGLE_EAD);

        URI uri = getImportUrl("ead", "r1", "Test", false)
                .queryParam(HANDLER_PARAM, "java.lang.String") // oops
                .queryParam(COMMIT_PARAM, true)
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.text(payloadStream), Response.class)) {

            assertStatus(Response.Status.BAD_REQUEST, response);
            String output = response.readEntity(String.class);
            System.out.println(output);
            JsonNode rootNode = jsonMapper.readTree(output);
            assertTrue("Has correct error messages", rootNode.path("details").toString()
                    .contains("not an instance of"));
        }
    }

    @Test
    public void testImportEadWithEmptyPayload() throws Exception {
        // Get the path of an EAD file
        URI uri = getImportUrl("ead", "r1", "Test", false)
                .queryParam(COMMIT_PARAM, true)
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.entity("", MediaType.APPLICATION_OCTET_STREAM_TYPE), Response.class)) {

            assertStatus(Response.Status.BAD_REQUEST, response);
            String output = response.readEntity(String.class);
            JsonNode rootNode = jsonMapper.readTree(output);
            assertTrue("Has correct error messages", rootNode.path("details").toString()
                    .contains("EOF reading input data"));
        }
    }

    @Test
    public void testImportEadWithFileLogMessage() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getResourcePathsPayloadStream(SINGLE_EAD);
        long count = getEntityCount(Entities.DOCUMENTARY_UNIT, getAdminUserProfileId());
        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", getTestLogFilePath(logText), false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(COMMIT_PARAM, true)
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.text(payloadStream), Response.class)) {

            ImportLog log = response.readEntity(ImportLog.class);
            assertEquals(1, log.getCreated());
            assertEquals(0, log.getUpdated());
            assertEquals(0, log.getUnchanged());
            assertEquals(logText, log.getLogMessage().orElse(null));
            assertThat(log.getEventId().orElse(null), notNullValue());
            assertEquals(count + 1L,
                    getEntityCount(Entities.DOCUMENTARY_UNIT, getAdminUserProfileId()));
        }
    }

    @Test
    public void testImportEadDryRun() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getResourcePathsPayloadStream(SINGLE_EAD);
        long count = getEntityCount(Entities.DOCUMENTARY_UNIT, getAdminUserProfileId());
        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", getTestLogFilePath(logText), false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(COMMIT_PARAM, false) // Not committing!
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.text(payloadStream), Response.class)) {

            ImportLog log = response.readEntity(ImportLog.class);
            assertEquals(1, log.getCreated());
            assertEquals(0, log.getUpdated());
            assertEquals(0, log.getUnchanged());
            assertEquals(logText, log.getLogMessage().orElse(null));
            assertThat(log.getEventId().orElse(null), notNullValue());
            // count should be the same as before since we didn't commit
            assertEquals(count,
                    getEntityCount(Entities.DOCUMENTARY_UNIT, getAdminUserProfileId()));
        }
    }

    @Test
    public void testImportEadWithMultipleFilesInZip() throws Exception {
        File temp = File.createTempFile("test-zip", ".zip");
        temp.deleteOnExit();
        createZipFromResources(temp, SINGLE_EAD, HIERARCHICAL_EAD);

        // Get the path of an EAD file
        InputStream payloadStream = Files.newInputStream(temp.toPath());

        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", getTestLogFilePath(logText), false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(COMMIT_PARAM, true)
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.entity(payloadStream, MediaType.APPLICATION_OCTET_STREAM_TYPE), Response.class)) {

            ImportLog log = response.readEntity(ImportLog.class);
            assertEquals(6, log.getCreated());
            assertEquals(0, log.getUpdated());
            assertEquals(0, log.getUnchanged());
            assertEquals(logText, log.getLogMessage().orElse(null));
            assertThat(log.getEventId().orElse(null), notNullValue());
        }
    }

    @Test
    public void testImportEadWithMultipleFilesInGZipTar() throws Exception {
        File temp = File.createTempFile("test-tar", ".tar");
        temp.deleteOnExit();
        File gzip = File.createTempFile("test-gzip", ".gz");
        createZipFromResources(temp, SINGLE_EAD, HIERARCHICAL_EAD);
        IOHelpers.gzipFile(temp.toPath(), gzip.toPath());

        // Get the path of an EAD file
        InputStream payloadStream = Files.newInputStream(gzip.toPath());

        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", getTestLogFilePath(logText), false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(COMMIT_PARAM, true)
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.entity(payloadStream, MediaType.APPLICATION_OCTET_STREAM_TYPE), Response.class)) {

            ImportLog log = response.readEntity(ImportLog.class);
            assertEquals(6, log.getCreated());
            assertEquals(0, log.getUpdated());
            assertEquals(0, log.getUnchanged());
            assertEquals(logText, log.getLogMessage().orElse(null));
            assertThat(log.getEventId().orElse(null), notNullValue());
        }
    }

    @Test
    public void testSyncEad() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getResourcePathsPayloadStream(HIERARCHICAL_EAD);

        String logText = "Setup";
        URI uri = getImportUrl("ead-sync", "r1", logText, false)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(COMMIT_PARAM, true)
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.text(payloadStream), Response.class)) {

            SyncLog log = response.readEntity(SyncLog.class);
            // this will have deleted all existing items in the repo...
            assertEquals(5, log.deleted().size());
            assertEquals(5, log.log().getCreated());
            assertEquals(0, log.log().getUpdated());
            assertEquals(0, log.log().getUnchanged());
            assertEquals(logText, log.log().getLogMessage().orElse(null));
        }

        // Now sync the updates file
        String logText2 = "Testing sync";
        URI uri2 = getImportUrl("ead-sync", "r1", logText2, false)
                .queryParam(ALLOW_UPDATES_PARAM, true)
                .queryParam(HANDLER_PARAM, EadHandler.class.getName())
                .queryParam(COMMIT_PARAM, true)
                .build();
        InputStream payloadStream2 = getResourcePathsPayloadStream("hierarchical-ead-sync-test.xml");
        try (Response response2 = callAs(getAdminUserProfileId(), uri2)
                .post(Entity.text(payloadStream2), Response.class)) {

            SyncLog log2 = response2.readEntity(SyncLog.class);
            System.out.println(log2);
            assertEquals(2, log2.log().getCreated());
            assertEquals(0, log2.log().getUpdated());
            assertEquals(3, log2.log().getUnchanged());
            assertEquals(logText2, log2.log().getLogMessage().orElse(null));
            assertThat(log2.log().getEventId().orElse(null), notNullValue());
        }
    }

    @Test
    public void testImportEag() {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("eag.xml");
        String logText = "Testing import";
        URI uri = getImportUrl("eag", "nl", logText, false)
                .queryParam(COMMIT_PARAM, true)
                .build();
        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.xml(payloadStream), Response.class)) {
            assertStatus(Response.Status.OK, response);
            ImportLog log = response.readEntity(ImportLog.class);
            assertEquals(1, log.getCreated());
            assertEquals(0, log.getUpdated());
            assertEquals(0, log.getUnchanged());
            assertEquals(logText, log.getLogMessage().orElse(null));
            assertThat(log.getEventId().orElse(null), notNullValue());
        }
    }

    @Test
    public void testImportEac() {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("eac.xml");
        String logText = "Testing import";
        URI uri = getImportUrl("eac", "auths", logText, false)
                .queryParam(COMMIT_PARAM, true)
                .build();

        try (Response response = callAs(getAdminUserProfileId(), uri)
                .post(Entity.xml(payloadStream), Response.class)) {
            assertStatus(Response.Status.OK, response);
            ImportLog log = response.readEntity(ImportLog.class);
            assertEquals(1, log.getCreated());
            assertEquals(0, log.getUpdated());
            assertEquals(0, log.getUnchanged());
            assertEquals(logText, log.getLogMessage().orElse(null));
            assertThat(log.getEventId().orElse(null), notNullValue());
        }
    }

    @Test
    public void testImportLinks() {
        Table table = Table.of(ImmutableList.of(
                ImmutableList.of("r1", "c1", "", "associative", "", "Test"),
                ImmutableList.of("r1", "c1", "ur1", "associative", "", "Test 2"),
                ImmutableList.of("r4", "c4", "", "associative", "", "Test 3")
        ));
        URI jsonUri = ehriUriBuilder(ImportResource.ENDPOINT, "links").build();
        try (Response response = callAs(getAdminUserProfileId(), jsonUri)
                .post(Entity.json(table), Response.class)) {
            ImportLog out = response.readEntity(ImportLog.class);
            assertEquals(3, out.getCreated());
        }
    }

    @Test
    public void testImportCoreferences() {
        Table table = Table.of(ImmutableList.of(
                ImmutableList.of("Subject Access 1", "a1"), // already exists
                ImmutableList.of("Person Access 2", "a1"),
                ImmutableList.of("Disconnected Access 1", "r4") // Updated
        ));
        URI jsonUri = ehriUriBuilder(ImportResource.ENDPOINT, "coreferences")
                .queryParam("scope", "r1")
                .queryParam("commit", "true")
                .build();

        try (Response response = callAs(getAdminUserProfileId(), jsonUri)
                .post(Entity.json(table), Response.class)) {
            ImportLog out = response.readEntity(ImportLog.class);
            assertEquals(1, out.getCreated());
            assertEquals(1, out.getUpdated());
        }
    }

    private UriBuilder getImportUrl(String endPoint, String scopeId, String log, boolean tolerant) {
        return ehriUriBuilder(ImportResource.ENDPOINT, endPoint)
                .queryParam(LOG_PARAM, log)
                .queryParam(SCOPE_PARAM, scopeId)
                .queryParam(TOLERANT_PARAM, String.valueOf(tolerant));
    }

    private InputStream getResourcePathsPayloadStream(String... resources) throws URISyntaxException {
        List<String> paths = Lists.newArrayList();
        for (String resourceName : resources) {
            URL resource = Resources.getResource(resourceName);
            paths.add(Paths.get(resource.toURI()).toAbsolutePath().toString());
        }
        String payloadText = Joiner.on("\n").join(paths) + "\n";
        return new ByteArrayInputStream(
                payloadText.getBytes(Charsets.UTF_8));
    }

    private InputStream getJsonObjectPayloadStream(Map<String, String> data)
            throws Exception {
        byte[] buf = jsonMapper.writer().writeValueAsBytes(data);
        return new ByteArrayInputStream(buf);
    }

    private String getTestLogFilePath(String text) throws IOException {
        File temp = File.createTempFile("test-log", ".tmp");
        temp.deleteOnExit();
        FileUtils.writeStringToFile(temp, text, Charsets.UTF_8);
        return temp.getAbsolutePath();
    }
}
