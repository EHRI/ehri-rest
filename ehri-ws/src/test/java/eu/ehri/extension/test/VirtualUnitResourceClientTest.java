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
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.UserProfileResource;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.ErrorSet;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static com.sun.jersey.api.client.ClientResponse.Status.BAD_REQUEST;
import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VirtualUnitResourceClientTest extends AbstractResourceClientTest {

    private String jsonVirtualUnitStr;
    private String partialJsonVirtualUnitTestStr;
    private static final String UPDATED_NAME = "UpdatedNameTEST";
    private static final String PARTIAL_DESC = "Changing the description";
    private static final String TEST_JSON_IDENTIFIER = "vc1";
    private static final String FIRST_DOC_ID = "vc1";
    private static final String CREATED_ID = "some-id-supplied-by-frontend";

    @Before
    public void setUp() throws Exception {
        jsonVirtualUnitStr = readResourceFileAsString("VirtualUnit.json");
        partialJsonVirtualUnitTestStr = readResourceFileAsString("partialVirtualUnit.json");
    }

    /**
     * CR(U)D cycle
     */
    @Test
    public void testCreateDeleteVirtualUnit() throws Exception {
        // Create
        String currentUserId = getAdminUserProfileId();
        ClientResponse response = jsonCallAs(currentUserId, getCreationUri())
                .entity(jsonVirtualUnitStr).post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();
        response = jsonCallAs(currentUserId, location)
                .get(ClientResponse.class);
        assertStatus(OK, response);

        // Ensure the user now owns that item:
        response = jsonCallAs(currentUserId,
                entityUri(Entities.USER_PROFILE, currentUserId,
                        "virtual-units"))
                .get(ClientResponse.class);

        assertStatus(OK, response);
        // Check the response contains a new version
        assertEquals(1, getPaginationTotal(response));
    }

    @Test
    public void testCreateDeleteChildVirtualUnit() throws Exception {
        // Create
        String currentUserId = getAdminUserProfileId();
        ClientResponse response = jsonCallAs(currentUserId,
                entityUri(Entities.VIRTUAL_UNIT, FIRST_DOC_ID))
                .entity(jsonVirtualUnitStr).post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();
        response = jsonCallAs(currentUserId, location)
                .get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testIntegrityError() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .entity(jsonVirtualUnitStr).post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Okay... now if we try and do the same things again we should
        // get an integrity error because the identifiers are the same.
        response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .entity(jsonVirtualUnitStr).post(ClientResponse.class);
        // Check the JSON gives use the correct error
        String errString = response.getEntity(String.class);

        assertStatus(BAD_REQUEST, response);

        JsonNode rootNode = jsonMapper.readTree(errString);
        JsonNode errValue = rootNode.path("details").path(ErrorSet.ERROR_KEY).path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
    }

    @Test
    public void testGetVirtualUnitByIdentifier() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.VIRTUAL_UNIT, TEST_JSON_IDENTIFIER))
                .get(ClientResponse.class);

        assertStatus(OK, response);

        JsonNode rootNode = jsonMapper.readTree(response.getEntity(String.class));
        JsonNode errValue = rootNode.path("data").path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
        assertEquals(TEST_JSON_IDENTIFIER, errValue.textValue());
    }

    @Test
    public void testUpdateVirtualUnitByIdentifier() throws Exception {
        // Update doc unit c1 with the test json values, which should change
        // its identifier to some-id
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.VIRTUAL_UNIT, TEST_JSON_IDENTIFIER))
                .entity(jsonVirtualUnitStr)
                .put(ClientResponse.class);

        assertStatus(OK, response);

        JsonNode rootNode = jsonMapper.readTree(response.getEntity(String.class));
        JsonNode errValue = rootNode.path("data").path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
        assertEquals(CREATED_ID, errValue.textValue());
    }

    @Test
    public void testListVirtualUnit() throws Exception {
        List<Bundle> data = getEntityList(
                Entities.VIRTUAL_UNIT, getAdminUserProfileId());
        assertTrue(!data.isEmpty());
        data.sort(bundleComparator);
        // Extract the first collection. According to the fixtures this
        // should be named 'vc1'.
        assertEquals(FIRST_DOC_ID, data.get(0).getDataValue(Ontology.IDENTIFIER_KEY));
    }

    @Test
    public void testCountVirtualUnits() throws Exception {
        Long data = getEntityCount(
                Entities.VIRTUAL_UNIT, getAdminUserProfileId());
        assertEquals(Long.valueOf(4), data);
    }

    @Test
    public void testUpdateVirtualUnit() throws Exception {

        // -create data for testing, making this a child element of c1.
        WebResource resource = client.resource(getCreationUri());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonVirtualUnitStr).post(ClientResponse.class);

        assertStatus(CREATED, response);
        assertValidJsonData(response);
        // response.getEntity(String.class)

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and change it
        String json = response.getEntity(String.class);
        String toUpdateJson = Bundle.fromString(json)
                .withDataValue("name", UPDATED_NAME).toJson();

        // -update
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(toUpdateJson)
                .put(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it changed?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.getEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(UPDATED_NAME, updatedEntityBundle.getDataValue("name"));
    }

    @Test
    public void testPatchVirtualUnit() throws Exception {

        // -create data for testing, making this a child element of c1.
        WebResource resource = client.resource(getCreationUri());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .entity(jsonVirtualUnitStr).post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Get created doc via the response location?
        URI location = response.getLocation();

        String toUpdateJson = partialJsonVirtualUnitTestStr;

        // - patch the data (using the Patch header)
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .header(AbstractResource.PATCH_HEADER_NAME, Boolean.TRUE.toString())
                .entity(toUpdateJson)
                .put(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it patched?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .get(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.getEntity(String.class);
        Bundle updatedEntityBundle = Bundle.fromString(updatedJson);
        assertEquals(CREATED_ID, updatedEntityBundle.getDataValue(Ontology.IDENTIFIER_KEY));
        assertEquals(PARTIAL_DESC, updatedEntityBundle.getDataValue("description"));
    }

    @Test
    public void testPageVirtualUnitsForUser() throws Exception {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.USER_PROFILE, "linda",
                        "virtual-units"))
                .get(ClientResponse.class);

        assertStatus(OK, response);
        assertEquals(1, getPaginationTotal(response));
    }

    private URI getCreationUri() {
        return entityUri(Entities.VIRTUAL_UNIT);
    }
}
