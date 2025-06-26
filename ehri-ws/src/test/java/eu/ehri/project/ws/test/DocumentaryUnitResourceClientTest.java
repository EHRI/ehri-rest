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
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.ErrorSet;
import eu.ehri.project.utils.Table;
import eu.ehri.project.ws.base.AbstractResource;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class DocumentaryUnitResourceClientTest extends AbstractResourceClientTest {

    private String jsonDocumentaryUnitTestStr;
    private String invalidJsonDocumentaryUnitTestStr;
    private String partialJsonDocumentaryUnitTestStr;
    private static final String UPDATED_NAME = "UpdatedNameTEST";
    private static final String PARTIAL_NAME = "PatchNameTest";
    private static final String TEST_JSON_IDENTIFIER = "c1";
    private static final String FIRST_DOC_ID = "c1";
    private static final String TEST_HOLDER_IDENTIFIER = "r1";
    private static final String CREATED_ID = "some-id";

    @Before
    public void setUp() throws Exception {
        jsonDocumentaryUnitTestStr = readResourceFileAsString("DocumentaryUnit.json");
        invalidJsonDocumentaryUnitTestStr = readResourceFileAsString("invalidDocumentaryUnit.json");
        partialJsonDocumentaryUnitTestStr = readResourceFileAsString("partialDocumentaryUnit.json");
    }

    /**
     * CR(U)D cycle
     */
    @Test
    public void testCreateDeleteDocumentaryUnit() throws Exception {
        // Create
        Response response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .post(Entity.json(jsonDocumentaryUnitTestStr), Response.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();
        response = jsonCallAs(getAdminUserProfileId(), location)
                .get(Response.class);
        assertStatus(OK, response);
    }

    @Test
    public void testNotFoundWithValidUrl() throws Exception {
        // Create
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, "r1"))
                .get(Response.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testCacheControl() throws Exception {
        // Create
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, "c1"))
                .get(Response.class);
        assertStatus(OK, response);
        String c1cc = response.getHeaderString(HttpHeaders.CACHE_CONTROL);
        assertThat(c1cc, containsString("no-cache"));
        assertThat(c1cc, containsString("no-store"));
        // C4 is unrestricted and thus has a max-age set
        response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, "c4"))
                .get(Response.class);
        assertStatus(OK, response);
        String c4cc = response.getHeaderString(HttpHeaders.CACHE_CONTROL);
        assertThat(c4cc, not(containsString("no-cache")));
        assertThat(c4cc, not(containsString("no-store")));
        assertThat(c4cc, containsString("max-age=" + AbstractResource.ITEM_CACHE_TIME));
    }

    @Test
    public void testDeleteDocumentaryWithChildren() throws Exception {
        // Create
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, FIRST_DOC_ID))
                .delete(Response.class);
        assertStatus(CONFLICT, response);
    }

    @Test
    public void testDeleteChildren() throws Exception {
        // Create
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUriBuilder(Entities.DOCUMENTARY_UNIT, FIRST_DOC_ID, "list")
                        .queryParam(AbstractResource.ALL_PARAM, "true")
                        .queryParam("batch", "2")
                        .build())
                .delete(Response.class);
        assertStatus(OK, response);

        Table expected = Table.of(Lists.newArrayList(
                Lists.newArrayList("c2"),
                Lists.newArrayList("c3")
        ));
        assertEquals(expected, response.readEntity(Table.class));
        for (List<String> id : expected.rows()) {
            Response r1 = jsonCallAs(getAdminUserProfileId(),
                    entityUri(Entities.DOCUMENTARY_UNIT, id.get(0)))
                    .head();
            assertStatus(GONE, r1);
        }
    }

    @Test
    public void testCreateDeleteChildDocumentaryUnit() throws Exception {
        // Create
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, FIRST_DOC_ID))
                .post(Entity.json(jsonDocumentaryUnitTestStr), Response.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();
        response = jsonCallAs(getAdminUserProfileId(), location)
                .get(Response.class);
        assertStatus(OK, response);
    }

    @Test
    public void testIntegrityError() throws Exception {
        // Create
        Response response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .post(Entity.json(jsonDocumentaryUnitTestStr), Response.class);

        assertStatus(CREATED, response);

        // Okay... now if we try and do the same things again we should
        // get an integrity error because the identifiers are the same.
        response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .post(Entity.json(jsonDocumentaryUnitTestStr), Response.class);
        // Check the JSON gives use the correct error
        String errString = response.readEntity(String.class);
        assertStatus(BAD_REQUEST, response);

        JsonNode rootNode = jsonMapper.readTree(errString);
        assertEquals(BAD_REQUEST.getReasonPhrase(), rootNode.path("error").asText());
        JsonNode errValue = rootNode.path("details").path(ErrorSet.ERROR_KEY).path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
    }

    @Test
    public void testValidationError() throws Exception {
        // Create
        Response response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .post(Entity.json(invalidJsonDocumentaryUnitTestStr), Response.class);

        String errorJson = response.readEntity(String.class);
        assertStatus(BAD_REQUEST, response);

        // Check the JSON gives use the correct error
        // In this case the start and end dates for the
        // first date relation should be missing
        JsonNode rootNode = jsonMapper.readTree(errorJson);
        JsonNode errValue1 = rootNode.path("details")
                .path(ErrorSet.REL_KEY)
                .path(Ontology.DESCRIPTION_FOR_ENTITY).path(0)
                .path(ErrorSet.ERROR_KEY).path(Ontology.NAME_KEY);
        assertFalse(errValue1.isMissingNode());
    }

    @Test
    public void testGetDocumentaryUnitByIdentifier() throws Exception {
        Bundle doc = getEntity(Entities.DOCUMENTARY_UNIT, TEST_JSON_IDENTIFIER,
                getAdminUserProfileId());
        assertEquals(TEST_JSON_IDENTIFIER, doc.getDataValue(Ontology.IDENTIFIER_KEY));
    }

    @Test
    public void testUpdateDocumentaryUnitByIdentifier() throws Exception {
        // Update doc unit c1 with the test json values, which should change
        // its identifier to some-id
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, TEST_JSON_IDENTIFIER))
                .put(Entity.json(jsonDocumentaryUnitTestStr), Response.class);

        assertStatus(OK, response);

        JsonNode rootNode = jsonMapper.readTree(response.readEntity(String.class));
        JsonNode errValue = rootNode.path("data").path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
        assertEquals(CREATED_ID, errValue.textValue());
    }

    @Test
    public void testListDocumentaryUnit() throws Exception {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add(AbstractResource.SORT_PARAM, Ontology.IDENTIFIER_KEY);
        List<Bundle> data = getEntityList(
                entityUri(Entities.DOCUMENTARY_UNIT), getAdminUserProfileId(), params);
        assertFalse(data.isEmpty());
        data.sort(bundleComparator);
        // Extract the first documentary unit. According to the fixtures this
        // should be named 'c1'.
        assertEquals(FIRST_DOC_ID, data.get(0).getDataValue(Ontology.IDENTIFIER_KEY));
    }

    @Test
    public void testListDocumentaryUnitWithStreaming() throws Exception {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        //params.add(AbstractResource.SORT_PARAM, Ontology.IDENTIFIER_KEY);
        params.add(AbstractResource.LIMIT_PARAM, String.valueOf(-1L));

        WebTarget resource = client.target(entityUri(Entities.DOCUMENTARY_UNIT));
        for (String key : params.keySet()) {
            resource = resource.queryParam(key, params.getFirst(key));
        }
        String s = resource
                .request(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .header(AbstractResource.STREAM_HEADER_NAME, "true")
                .get(String.class);

        List<Bundle> data = decodeList(s);
        assertFalse(data.isEmpty());
        data.sort(bundleComparator);
        // Extract the first documentary unit. According to the fixtures this
        // should be named 'c1'.
        assertEquals(FIRST_DOC_ID, data.get(0).getDataValue(Ontology.IDENTIFIER_KEY));
    }

    @Test
    public void testListDocumentaryUnitWithNotFound() throws Exception {
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, "BAD_ID", "list"))
                .get(Response.class);
        String s = response.readEntity(String.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testListDocumentaryUnitWithBadUser() throws Exception {
        Response response = jsonCallAs("invalidId",
                entityUri(Entities.DOCUMENTARY_UNIT, TEST_JSON_IDENTIFIER, "list"))
                .get(Response.class);
        assertStatus(BAD_REQUEST, response);
    }

    @Test
    public void testListDocumentaryUnitWithOffset() throws Exception {
        // Fetch the second doc unit item (c2)
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add(AbstractResource.OFFSET_PARAM, "1");
        params.add(AbstractResource.LIMIT_PARAM, "1");
        params.add(AbstractResource.SORT_PARAM, Ontology.IDENTIFIER_KEY);
        List<Bundle> data = getEntityList(
                entityUri(Entities.DOCUMENTARY_UNIT), getAdminUserProfileId(), params);
        assertEquals(1, data.size());
        data.sort(bundleComparator);
        // Extract the second documentary unit. According to the fixtures this
        // should be named 'c2'.
        assertEquals("c2", data.get(0).getDataValue(Ontology.IDENTIFIER_KEY));
    }

    @Test
    public void testCountDocumentaryUnits() throws Exception {
        Long data = getEntityCount(
                Entities.DOCUMENTARY_UNIT, getAdminUserProfileId());
        assertEquals(Long.valueOf(5), data);
    }

    @Test
    public void testUpdateDocumentaryUnit() throws Exception {

        // -create data for testing, making this a child element of c1.
        WebTarget resource = client.target(getCreationUri());
        Response response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(Entity.json(jsonDocumentaryUnitTestStr), Response.class);

        assertStatus(CREATED, response);
        assertValidJsonData(response);
        // response.getEntity(String.class)

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.target(location);
        response = resource
                .request(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(Response.class);
        assertStatus(OK, response);

        // -get the data and change it
        String json = response.readEntity(String.class);
        String toUpdateJson = Bundle.fromString(json)
                .withDataValue("name", UPDATED_NAME).toJson();

        // -update
        resource = client.target(location);
        response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .put(Entity.json(toUpdateJson), Response.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it changed?
        resource = client.target(location);
        response = resource
                .request(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(Response.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.readEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(UPDATED_NAME, updatedEntityBundle.getDataValue("name"));
    }

    @Test
    public void testPatchDocumentaryUnit() throws Exception {

        // -create data for testing, making this a child element of c1.
        WebTarget resource = client.target(getCreationUri());
        Response response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(Entity.json(jsonDocumentaryUnitTestStr), Response.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();

        String toUpdateJson = partialJsonDocumentaryUnitTestStr;

        // - patch the data (using the Patch header)
        resource = client.target(location);
        response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .header(AbstractResource.PATCH_HEADER_NAME, Boolean.TRUE.toString())
                .put(Entity.json(toUpdateJson), Response.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it patched?
        resource = client.target(location);
        response = resource
                .request(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(Response.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.readEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(CREATED_ID, updatedEntityBundle.getDataValue(Ontology.IDENTIFIER_KEY));
        assertEquals(PARTIAL_NAME, updatedEntityBundle.getDataValue(Ontology.NAME_KEY));
    }

    @Test
    public void testRenameDocumentaryUnitWithCollision() throws Exception {
        // When there's a confict the result should be an HTTP 409 error
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, "c1", "rename"))
                .post(Entity.text("m19"), Response.class);
        assertStatus(CONFLICT, response);

        // When the check parameter is given the result should be a list
        // of conflicting item IDs
        Response response2 = jsonCallAs(getAdminUserProfileId(),
                entityUriBuilder(Entities.DOCUMENTARY_UNIT, "c1", "rename")
                        .queryParam("check", true).build())
                .post(Entity.text("m19"), Response.class);
        assertStatus(OK, response2);
        Table expected = Table.of(Lists.<List<String>>newArrayList(
                Lists.newArrayList("c1", "nl-r1-m19")
        ));
        assertEquals(expected, response2.readEntity(Table.class));
    }

    @Test
    public void testRenameDocumentaryUnit() throws Exception {
        Response response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, "c1", "rename"))
                .post(Entity.text("z1"), Response.class);
        assertStatus(OK, response);
        Table expected = Table.of(Lists.newArrayList(
                Lists.newArrayList("c1", "nl-r1-z1"),
                Lists.newArrayList("c2", "nl-r1-z1-c2"),
                Lists.newArrayList("c3", "nl-r1-z1-c2-c3")
        ));
        assertEquals(expected, response.readEntity(Table.class));
    }

    @Test
    public void testExportEad() throws Exception {
        Response response = callAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, "c1", "ead"))
                .accept(MediaType.TEXT_XML_TYPE)
                .get(Response.class);
        String ead = response.readEntity(String.class);
        assertStatus(OK, response);
        assertThat(ead, containsString("<ead xmlns=\"urn:isbn:1-931666-22-9\""));

        // Now check EAD3
        response = callAs(getAdminUserProfileId(),
                entityUri(Entities.DOCUMENTARY_UNIT, "c1", "ead3"))
                .accept(MediaType.TEXT_XML_TYPE)
                .get(Response.class);
        assertStatus(OK, response);
        String ead3 = response.readEntity(String.class);
        assertThat(ead3, containsString("<ead xmlns=\"http://ead3.archivists.org/schema/\""));
    }

    private URI getCreationUri() {
        return entityUri(Entities.REPOSITORY, TEST_HOLDER_IDENTIFIER);
    }
}
