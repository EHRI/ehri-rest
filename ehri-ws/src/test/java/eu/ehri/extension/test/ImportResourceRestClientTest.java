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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.extension.ImportResource;
import eu.ehri.project.importers.ead.IcaAtomEadHandler;
import org.apache.commons.io.FileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static eu.ehri.extension.ImportResource.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ImportResourceRestClientTest extends BaseRestClientTest {
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
        String output = response.getEntity(String.class);
        JsonNode rootNode = jsonMapper.readTree(output);
        assertEquals(1, rootNode.path("created").asInt());
        assertEquals(0, rootNode.path("updated").asInt());
        assertEquals(0, rootNode.path("unchanged").asInt());
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
                .header("Content-Type", "text/plain")
                .entity(payloadStream)
                .post(ClientResponse.class);

        assertStatus(ClientResponse.Status.OK, response);
        String output = response.getEntity(String.class);

        JsonNode rootNode = jsonMapper.readTree(output);
        assertEquals(1, rootNode.path("created").asInt());
        assertEquals(0, rootNode.path("updated").asInt());
        assertEquals(0, rootNode.path("unchanged").asInt());
        assertEquals(logText, rootNode.path("message").asText());
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
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .header("Content-Type", "text/xml")
                .entity(payloadStream)
                .post(ClientResponse.class);

        assertStatus(ClientResponse.Status.OK, response);
        String output = response.getEntity(String.class);

        JsonNode rootNode = jsonMapper.readTree(output);
        assertEquals(1, rootNode.path("created").asInt());
        assertEquals(0, rootNode.path("updated").asInt());
        assertEquals(0, rootNode.path("unchanged").asInt());
        assertEquals(logText, rootNode.path("message").asText());
    }

    @Test
    public void testImportEadWithNonExistentClass() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getPayloadStream(SINGLE_EAD);

        URI uri = getImportUrl("ead", "r1", "Test", false)
                .queryParam(HANDLER_PARAM, "IDontExist") // oops
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .header("Content-Type", "text/plain")
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
                .header("Content-Type", "text/plain")
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
                .header("Content-Type", "text/plain")
                .entity(payloadStream)
                .post(ClientResponse.class);

        assertStatus(ClientResponse.Status.OK, response);
        String output = response.getEntity(String.class);

        JsonNode rootNode = jsonMapper.readTree(output);
        assertEquals(1, rootNode.path("created").asInt());
        assertEquals(0, rootNode.path("updated").asInt());
        assertEquals(0, rootNode.path("unchanged").asInt());
        assertEquals(logText, rootNode.path("message").asText());
    }

    @Test
    public void testImportEadWithMultipleFilesInZip() throws Exception {
        File temp = File.createTempFile("test-zip", ".zip");
        temp.deleteOnExit();
        createZip(temp, SINGLE_EAD, HIERARCHICAL_EAD);

        // Get the path of an EAD file
        InputStream payloadStream = new FileInputStream(temp);

        String logText = "Testing import";
        URI uri = getImportUrl("ead", "r1", getTestLogFilePath(logText), false)
                .queryParam(HANDLER_PARAM, IcaAtomEadHandler.class.getName())
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .header("Content-Type", "application/octet-stream")
                .entity(payloadStream)
                .post(ClientResponse.class);

        String output = response.getEntity(String.class);
        System.out.println(output);
        assertStatus(ClientResponse.Status.OK, response);

        JsonNode rootNode = jsonMapper.readTree(output);
        assertEquals(6, rootNode.path("created").asInt());
        assertEquals(0, rootNode.path("updated").asInt());
        assertEquals(0, rootNode.path("unchanged").asInt());
        assertEquals(logText, rootNode.path("message").asText());
    }

    @Test
    public void testImportEag() throws Exception {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("eag.xml");
        String logText = "Testing import";
        URI uri = getImportUrl("eag", "nl", logText, false)
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .header("Content-Type", "text/xml")
                .entity(payloadStream)
                .post(ClientResponse.class);

        String output = response.getEntity(String.class);
        System.out.println(output);
        assertStatus(ClientResponse.Status.OK, response);

        JsonNode rootNode = jsonMapper.readTree(output);
        assertEquals(1, rootNode.path("created").asInt());
        assertEquals(0, rootNode.path("updated").asInt());
        assertEquals(0, rootNode.path("unchanged").asInt());
        assertEquals(logText, rootNode.path("message").asText());
    }

    @Test
    public void testImportEac() throws Exception {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("eac.xml");
        String logText = "Testing import";
        URI uri = getImportUrl("eac", "auths", logText, false)
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .header("Content-Type", "text/xml")
                .entity(payloadStream)
                .post(ClientResponse.class);

        String output = response.getEntity(String.class);
        System.out.println(output);
        assertStatus(ClientResponse.Status.OK, response);

        JsonNode rootNode = jsonMapper.readTree(output);
        assertEquals(1, rootNode.path("created").asInt());
        assertEquals(0, rootNode.path("updated").asInt());
        assertEquals(0, rootNode.path("unchanged").asInt());
        assertEquals(logText, rootNode.path("message").asText());
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

    private void createZip(File file, String... resources)
            throws URISyntaxException, IOException {
        FileOutputStream fos = new FileOutputStream(file);
        ZipOutputStream zos = new ZipOutputStream(fos);
        for (String resource : resources) {
            URL url = Resources.getResource(resource);
            String name = new File(url.toURI()).getAbsolutePath();
            zos.putNextEntry(new ZipEntry(name));
            Resources.copy(url, zos);
            zos.closeEntry();
        }

        zos.close();
    }

    private String getTestLogFilePath(String text) throws IOException {
        File temp = File.createTempFile("test-log", ".tmp");
        temp.deleteOnExit();
        FileUtils.writeStringToFile(temp, text);
        return temp.getAbsolutePath();
    }
}
