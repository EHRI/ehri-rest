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
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.ErrorSet;
import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.sun.jersey.api.client.ClientResponse.Status.*;
import static org.junit.Assert.*;

public class VirtualUnitRestClientTest extends BaseRestClientTest {

    private String jsonVirtualUnitStr;
    private String partialJsonVirtualUnitTestStr;
    static final String UPDATED_NAME = "UpdatedNameTEST";
    static final String PARTIAL_DESC = "Changing the description";
    static final String TEST_JSON_IDENTIFIER = "vc1";
    static final String FIRST_DOC_ID = "vc1";
    // FIXME: This ID is temporaty and will break when we decide on a proper
    // prefix ID scheme
    static final String CREATED_ID = "some-id-supplied-by-frontend";

    @Before
    public void setUp() throws Exception {
        jsonVirtualUnitStr = readResourceFileAsString("virtualUnit.json");
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
                ehriUri(Entities.VIRTUAL_UNIT, "forUser", currentUserId))
                .get(ClientResponse.class);

        assertStatus(OK, response);
        // Check the response contains a new version
        assertEquals(Integer.valueOf(1), getPaginationTotal(response));
    }

    @Test
    public void testCreateDeleteChildVirtualUnit() throws Exception {
        // Create
        String currentUserId = getAdminUserProfileId();
        ClientResponse response = jsonCallAs(currentUserId,
                ehriUri(Entities.VIRTUAL_UNIT, FIRST_DOC_ID, Entities.VIRTUAL_UNIT))
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

        JsonNode rootNode = jsonMapper.readValue(errString, JsonNode.class);
        JsonNode errValue = rootNode.path(ErrorSet.ERROR_KEY).path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
    }

    @Test
    public void testGetVirtualUnitByIdentifier() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.VIRTUAL_UNIT, TEST_JSON_IDENTIFIER))
                .get(ClientResponse.class);

        assertStatus(OK, response);

        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode errValue = rootNode.path("data").path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
        assertEquals(TEST_JSON_IDENTIFIER, errValue.getTextValue());
    }

    @Test
    public void testUpdateVirtualUnitByIdentifier() throws Exception {
        // Update doc unit c1 with the test json values, which should change
        // its identifier to some-id
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.VIRTUAL_UNIT, TEST_JSON_IDENTIFIER))
                .entity(jsonVirtualUnitStr)
                .put(ClientResponse.class);

        assertStatus(OK, response);

        JsonNode rootNode = jsonMapper.readValue(response.getEntity(String.class),
                JsonNode.class);
        JsonNode errValue = rootNode.path("data").path(
                Ontology.IDENTIFIER_KEY);
        assertFalse(errValue.isMissingNode());
        assertEquals(CREATED_ID, errValue.getTextValue());
    }

    @Test
    public void testListVirtualUnit() throws Exception {
        List<Map<String, Object>> data = getEntityList(
                Entities.VIRTUAL_UNIT, getAdminUserProfileId());
        assertTrue(!data.isEmpty());
        Collections.sort(data, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> c1, Map<String, Object> c2) {
                return ((String) c1.get("id")).compareTo((String) c2.get("id"));
            }
        });
        // Extract the first collection. According to the fixtures this
        // should be named 'vc1'.
        @SuppressWarnings("unchecked")
        Map<String, Object> c1data = (Map<String, Object>) data.get(0).get(
                "data");
        assertEquals(FIRST_DOC_ID, c1data.get(Ontology.IDENTIFIER_KEY));
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
                .header(AbstractRestResource.AUTH_HEADER_NAME,
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
                .header(AbstractRestResource.AUTH_HEADER_NAME,
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
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(toUpdateJson)
                .put(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it changed?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
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
                .header(AbstractRestResource.AUTH_HEADER_NAME,
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
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .header(AbstractRestResource.PATCH_HEADER_NAME, Boolean.TRUE.toString())
                .entity(toUpdateJson)
                .put(ClientResponse.class);
        assertStatus(OK, response);

        // -get the data and convert to a bundle, is it patched?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
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
                ehriUri(Entities.VIRTUAL_UNIT, "forUser", "linda"))
                .get(ClientResponse.class);

        assertStatus(OK, response);
        assertEquals(Integer.valueOf(1), getPaginationTotal(response));
    }

    private URI getCreationUri() {
        return ehriUri(Entities.VIRTUAL_UNIT);
    }
}
