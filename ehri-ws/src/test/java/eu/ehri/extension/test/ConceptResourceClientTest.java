/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.project.definitions.Entities;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;
import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.NO_CONTENT;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ConceptResourceClientTest extends AbstractResourceClientTest {
    private static final String TEST_CVOC_ID = "cvoc1"; // vocabulary in fixture
    private static final String TEST_CVOC_CONCEPT_ID = "cvocc1";

    private String jsonApplesTestStr;

    @Before
    public void setUp() throws Exception {
        jsonApplesTestStr = readResourceFileAsString("apples.json");
    }

    /**
     * CR(U)D cycle
     */
    @Test
    public void testCreateDeleteCvocConcept() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                getCreationUri()).entity(jsonApplesTestStr)
                .post(ClientResponse.class);

        assertStatus(CREATED, response);

        // Get created entity via the response location?
        URI location = response.getLocation();

        response = jsonCallAs(getAdminUserProfileId(), location)
                .get(ClientResponse.class);
        assertStatus(OK, response);

        // Where is my deletion test, I want to know if it works
        response = jsonCallAs(getAdminUserProfileId(), location)
                .delete(ClientResponse.class);
        assertStatus(NO_CONTENT, response);

    }

    @Test
    public void testNarrowerCvocConcepts() throws Exception {
        String jsonFruitTestStr = "{\"type\":\"CvocConcept\", \"data\":{\"identifier\": \"fruit\"}}";
        String jsonAppleTestStr = "{\"type\":\"CvocConcept\", \"data\":{\"identifier\": \"apple\"}}";

        // Create fruit
        ClientResponse response = testCreateConcept(jsonFruitTestStr);
        // Get created entity via the response location
        URI fruitLocation = response.getLocation();

        // Create apple
        response = testCreateConcept(jsonAppleTestStr);

        // Get created entity via the response location
        URI appleLocation = response.getLocation();
        // ... ehh get the id number ... we need to fix this!
        String appleLocationStr = appleLocation.getPath();
        String appleIdStr = appleLocationStr.substring(appleLocationStr
                .lastIndexOf('/') + 1);

        // make apple narrower of fruit
        WebResource resource = client.resource(getRelationUri(fruitLocation, "narrower",
                appleIdStr));
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).post(ClientResponse.class);
        // Hmm, it's a post request, but we don't create a vertex (but we do an
        // edge...)
        assertStatus(OK, response);

        // get fruit's narrower concepts
        response = testGet(fruitLocation + "/list");
        // check if apple is in there
        assertTrue(containsIdentifier(response, "apple"));

        // check if apple's broader is fruit
        // get apple's broader concepts
        response = testGet(entityUri(Entities.CVOC_CONCEPT, appleIdStr, "broader"));
        // check if fruit is in there
        assertTrue(containsIdentifier(response, "fruit"));

        // Test removal of one narrower concept
        resource = client.resource(getRelationUri(fruitLocation, "narrower", appleIdStr));
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).delete(ClientResponse.class);
        assertStatus(OK, response);

        // apple should still exist
        response = testGet(entityUri(Entities.CVOC_CONCEPT, appleIdStr));
        assertStatus(OK, response);

        // but not as a narrower of fruit!
        response = testGet(fruitLocation);
        // check if apple is NOT in there
        assertFalse(containsIdentifier(response, "apple"));
    }

    @Test
    public void testRelatedCvocConcepts() throws Exception {
        String jsonTreeTestStr = "{\"type\":\"CvocConcept\", \"data\":{\"identifier\": \"tree\"}}";
        String jsonAppleTestStr = "{\"type\":\"CvocConcept\", \"data\":{\"identifier\": \"apple\"}}";

        // Create fruit
        ClientResponse response = testCreateConcept(jsonTreeTestStr);
        // Get created entity via the response location
        URI treeLocation = response.getLocation();

        // Create apple
        response = testCreateConcept(jsonAppleTestStr);

        // Get created entity via the response location
        URI appleLocation = response.getLocation();
        // ... ehh get the id number ... we need to fix this!
        String appleLocationStr = appleLocation.getPath();
        String appleIdStr = appleLocationStr.substring(appleLocationStr
                .lastIndexOf('/') + 1);

        // make apple related of fruit
        URI related = getRelationUri(treeLocation, "related", appleIdStr);
        WebResource resource = client.resource(related);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).post(ClientResponse.class);
        // Hmm, it's a post request, but we don't create a vertex (but we do an
        // edge...)
        assertStatus(OK, response);

        // get fruit's related concepts
        response = testGet(treeLocation + "/related");
        // check if apple is in there
        assertTrue(containsIdentifier(response, "apple"));

        // check if apple's relatedBy is tree
        // get apple's broader concepts
        response = testGet(entityUri(Entities.CVOC_CONCEPT,
                appleIdStr, "relatedBy"));
        // check if tree is in there
        assertTrue(containsIdentifier(response, "tree"));

        // Test removal of one related concept
        resource = client.resource(related);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).delete(ClientResponse.class);
        assertStatus(OK, response);

        // apple should still exist
        response = testGet(entityUri(Entities.CVOC_CONCEPT, appleIdStr));
        assertStatus(OK, response);

        // but not as a related of tree!
        response = testGet(treeLocation + "/related");
        // check that apple is NOT in there
        assertFalse(containsIdentifier(response, "apple"));
    }

    @Test
    public void testGetConcept() throws Exception {
        testGet(entityUri(Entities.CVOC_CONCEPT, TEST_CVOC_CONCEPT_ID));
    }

    @Test
    public void testDeleteConcept() throws Exception {
        URI url = entityUri(Entities.CVOC_CONCEPT, TEST_CVOC_CONCEPT_ID);
        testDelete(url);

        // Check it's really gone...
        WebResource resource = client.resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    /**
     * helpers **
     */

    private boolean containsIdentifier(final ClientResponse response,
            final String idStr) throws IOException {
        String json = response.getEntity(String.class);
        JsonNode rootNode = jsonMapper.readTree(json);
        JsonNode idPath = rootNode.path(0).path("data").path("identifier");
        return idPath.isTextual() && idPath.asText().equals(idStr);

    }

    private ClientResponse testGet(URI url) {
        return testGet(url.toString());
    }

    private ClientResponse testGet(String url) {
        WebResource resource = client.resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(ClientResponse.class);
        assertStatus(OK, response);

        return response;
    }

    private ClientResponse testDelete(URI url) {
        WebResource resource = client.resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .delete(ClientResponse.class);
        assertStatus(NO_CONTENT, response);

        return response;
    }

    private ClientResponse testCreateConcept(String json) {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .entity(json)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);
        return response;
    }

    private URI getRelationUri(URI base, String relation, String... related) {
        UriBuilder uriBuilder = UriBuilder.fromUri(base).segment(relation);
        for (String rel : related) {
            uriBuilder = uriBuilder.queryParam("id", rel);
        }
        return uriBuilder.build();
    }

    private URI getCreationUri() {
        // always create Concepts under a Vocabulary
        return entityUri(Entities.CVOC_VOCABULARY, TEST_CVOC_ID);
    }
}
