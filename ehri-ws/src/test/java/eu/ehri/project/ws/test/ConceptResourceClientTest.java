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

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.WebTarget;
import eu.ehri.project.ws.base.AbstractResource;
import eu.ehri.project.definitions.Entities;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.*;
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
        Response response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .post(Entity.json(jsonApplesTestStr), Response.class);

        assertStatus(CREATED, response);

        // Get created entity via the response location?
        URI location = response.getLocation();

        response = jsonCallAs(getAdminUserProfileId(), location)
                .get(Response.class);
        assertStatus(OK, response);

        // Where is my deletion test, I want to know if it works
        response = jsonCallAs(getAdminUserProfileId(), location)
                .delete(Response.class);
        assertStatus(NO_CONTENT, response);

    }

    @Test
    public void testNarrowerCvocConcepts() throws Exception {
        String jsonFruitTestStr = "{\"type\":\"CvocConcept\", \"data\":{\"identifier\": \"fruit\"}}";
        String jsonAppleTestStr = "{\"type\":\"CvocConcept\", \"data\":{\"identifier\": \"apple\"}}";

        // Create fruit
        Response response = testCreateConcept(jsonFruitTestStr);
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
        WebTarget resource = client.target(getRelationUri(fruitLocation, "narrower",
                appleIdStr));
        response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .post(Entity.json(""), Response.class);
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
        resource = client.target(getRelationUri(fruitLocation, "narrower", appleIdStr));
        response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .delete(Response.class);
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
        Response response = testCreateConcept(jsonTreeTestStr);
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
        WebTarget resource = client.target(related);
        response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .post(Entity.json(""), Response.class);
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
        resource = client.target(related);
        response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .delete(Response.class);
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
        WebTarget resource = client.target(url);
        Response response = resource
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .get(Response.class);
        assertStatus(GONE, response);
    }

    /**
     * helpers **
     */

    private boolean containsIdentifier(final Response response,
            final String idStr) throws IOException {
        String json = response.readEntity(String.class);
        JsonNode rootNode = jsonMapper.readTree(json);
        JsonNode idPath = rootNode.path(0).path("data").path("identifier");
        return idPath.isTextual() && idPath.asText().equals(idStr);

    }

    private Response testGet(URI url) {
        return testGet(url.toString());
    }

    private Response testGet(String url) {
        WebTarget resource = client.target(url);
        Response response = resource
                .request(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).get(Response.class);
        assertStatus(OK, response);

        return response;
    }

    private Response testDelete(URI url) {
        WebTarget resource = client.target(url);
        Response response = resource
                .request(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME, getAdminUserProfileId())
                .delete(Response.class);
        assertStatus(NO_CONTENT, response);

        return response;
    }

    private Response testCreateConcept(String json) {
        // Create
        Response response = jsonCallAs(getAdminUserProfileId(), getCreationUri())
                .post(Entity.json(json), Response.class);
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
