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
import eu.ehri.extension.VocabularyResource;
import eu.ehri.project.definitions.Entities;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.*;
import static org.junit.Assert.assertEquals;


public class CvocVocabularyClientTest extends BaseRestClientTest {
    static final String TEST_CVOC_ID = "cvoc1"; // vocabulary in fixture

    private String jsonTestVocabularyString = "{\"type\":\"" + Entities.CVOC_VOCABULARY +
            "\",\"data\":{\"identifier\": \"plants\", \"name\": \"Plants\"}}";
    private String jsonApplesTestStr;

    @Before
    public void setUp() throws Exception {
        jsonApplesTestStr = readResourceFileAsString("apples.json");
    }

    /**
     * CR(U)D cycle
     */
    @Test
    public void testCreateDeleteCvocVocabulary() throws Exception {
        String jsonTestString = jsonTestVocabularyString;

        // Create
        ClientResponse response = testCreate(ehriUri(Entities.CVOC_VOCABULARY),
                jsonTestString);

        // Get created entity via the response location?
        URI location = response.getLocation();

        response = testGet(location);
        assertStatus(OK, response);

        response = testDelete(location);
        assertStatus(OK, response);
    }

    // Test adding a concept, 
    // more concept testing is done elsewere
    @Test
    public void testCreateCvocConcept() throws Exception {
        // Create
        ClientResponse response = testCreate(ehriUri(Entities.CVOC_VOCABULARY),
                jsonTestVocabularyString);

        // Get created entity via the response location?
        URI location = response.getLocation();
        //... ehh get the id  ... we need to fix this!
        String locationStr = location.getPath();
        String vocIdStr = locationStr.substring(locationStr.lastIndexOf('/') + 1);

        response = testCreate(
                ehriUri(Entities.CVOC_VOCABULARY, vocIdStr, Entities.CVOC_CONCEPT),
                jsonApplesTestStr);
        location = response.getLocation();

        response = testGet(location);
        assertStatus(OK, response);

        response = testDelete(location);
        assertStatus(OK, response);
    }

    @Test
    public void testGetVocabulary() throws Exception {
        testGet(ehriUri(Entities.CVOC_VOCABULARY, TEST_CVOC_ID));
    }

    @Test
    public void testExportVocabulary() throws Exception {
        UriBuilder uri = ehriUriBuilder(Entities.CVOC_VOCABULARY, TEST_CVOC_ID, "export");
        ClientResponse response = client.resource(uri.build())
                .get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals(MediaType.valueOf(VocabularyResource.TURTLE_MIMETYPE + "; charset=utf-8"), response.getType());

        response = client.resource(uri.build())
                .header("Accept", VocabularyResource.RDF_XML_MIMETYPE)
                .get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals(MediaType.valueOf(VocabularyResource.RDF_XML_MIMETYPE + "; charset=utf-8"), response.getType());

        response = client.resource(uri.build())
                .header("Accept", VocabularyResource.N3_MIMETYPE)
                .get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals(MediaType.valueOf(VocabularyResource.N3_MIMETYPE + "; charset=utf-8"), response.getType());

        response = client.resource(uri.build())
                .queryParam("format", "N3")
                .get(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals(MediaType.valueOf(VocabularyResource.N3_MIMETYPE + "; charset=utf-8"), response.getType());
    }

    @Test
    public void testDeleteVocabularyTerms() throws Exception {
        URI uri = ehriUri(Entities.CVOC_VOCABULARY, TEST_CVOC_ID, "all");
        testDelete(uri);

        // Check it's really gone...
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                ehriUri(Entities.CVOC_CONCEPT, "cvocc1"))
                .get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testDeleteVocabulary() throws Exception {
        URI uri = ehriUri(Entities.CVOC_VOCABULARY, TEST_CVOC_ID);
        testDelete(uri);

        // Check it's really gone...
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    /**
     * helpers **
     */

    // Note: maybe generalize them and reuse for other tests
    public ClientResponse testGet(URI uri) {
        return jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
    }

    public ClientResponse testCreate(URI uri, String json) {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(json).post(ClientResponse.class);
        assertStatus(CREATED, response);
        return response;
    }

    public ClientResponse testDelete(URI uri) {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .delete(ClientResponse.class);
        assertStatus(OK, response);

        return response;
    }
}
