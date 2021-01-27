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

import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.extension.VocabularyResource;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.utils.Table;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.*;
import static eu.ehri.extension.base.AbstractResource.ALL_PARAM;
import static org.junit.Assert.assertEquals;


public class VocabularyResourceClientTest extends AbstractResourceClientTest {
    static final String TEST_CVOC_ID = "cvoc1"; // vocabulary in fixture

    private static final String jsonTestVocabularyString = "{\"type\":\"" + Entities.CVOC_VOCABULARY +
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

        // Create
        ClientResponse response = testCreate(entityUri(Entities.CVOC_VOCABULARY), jsonTestVocabularyString);

        // Get created entity via the response location?
        URI location = response.getLocation();

        response = testGet(location);
        assertStatus(OK, response);

        response = testDelete(location);
        assertStatus(NO_CONTENT, response);
    }

    // Test adding a concept, 
    // more concept testing is done elsewere
    @Test
    public void testCreateCvocConcept() throws Exception {
        // Create
        ClientResponse response = testCreate(entityUri(Entities.CVOC_VOCABULARY),
                jsonTestVocabularyString);

        // Get created entity via the response location?
        URI location = response.getLocation();
        //... ehh get the id  ... we need to fix this!
        String locationStr = location.getPath();
        String vocIdStr = locationStr.substring(locationStr.lastIndexOf('/') + 1);

        response = testCreate(entityUri(Entities.CVOC_VOCABULARY, vocIdStr), jsonApplesTestStr);
        location = response.getLocation();

        response = testGet(location);
        assertStatus(OK, response);

        response = testDelete(location);
        assertStatus(NO_CONTENT, response);
    }

    @Test
    public void testGetVocabulary() throws Exception {
        testGet(entityUri(Entities.CVOC_VOCABULARY, TEST_CVOC_ID));
    }

    @Test
    public void testExportVocabulary() throws Exception {
        UriBuilder uri = ehriUriBuilder(AbstractResource.RESOURCE_ENDPOINT_PREFIX,
                Entities.CVOC_VOCABULARY, TEST_CVOC_ID, "export");
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
    public void testDeleteChildren() throws Exception {
        URI uri = entityUriBuilder(Entities.CVOC_VOCABULARY, TEST_CVOC_ID, "list")
                .queryParam(ALL_PARAM, "true").build();
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .delete(ClientResponse.class);
        assertStatus(OK, response);
        assertEquals(
                Table.column(ImmutableList.of("cvocc1", "cvocc2")),
                response.getEntity(Table.class));

        // Check it's really gone...
        response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.CVOC_CONCEPT, "cvocc1"))
                .get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testDelete() throws Exception {
        // Check we can't delete with children
        URI uri = entityUri(Entities.CVOC_VOCABULARY, TEST_CVOC_ID);
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .delete(ClientResponse.class);
        assertStatus(CONFLICT, response);

        // Need to delete children first...
        testDelete(entityUri(Entities.CVOC_CONCEPT, "cvocc1"));
        testDelete(entityUri(Entities.CVOC_CONCEPT, "cvocc2"));

        // Now we should be able to delete it...
        testDelete(uri);

        // Check it's really gone...
        response = jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    /**
     * helpers **
     */

    // Note: maybe generalize them and reuse for other tests
    private ClientResponse testGet(URI uri) {
        return jsonCallAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
    }

    private ClientResponse testCreate(URI uri, String json) {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .entity(json).post(ClientResponse.class);
        assertStatus(CREATED, response);
        return response;
    }

    private ClientResponse testDelete(URI uri) {
        ClientResponse response = jsonCallAs(getAdminUserProfileId(), uri)
                .delete(ClientResponse.class);
        assertStatus(NO_CONTENT, response);

        return response;
    }
}
