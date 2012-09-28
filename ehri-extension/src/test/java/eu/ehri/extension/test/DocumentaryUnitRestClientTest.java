package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.research.ws.wadl.Doc;

import eu.ehri.extension.EhriNeo4jFramedResource;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.persistance.EntityBundle;

public class DocumentaryUnitRestClientTest extends BaseRestClientTest {

    private String jsonDocumentaryUnitTestStr; // test data to create a
                                               // DocumentaryUnit
    static final String UPDATED_NAME = "UpdatedNameTEST";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(DocumentaryUnitRestClientTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        jsonDocumentaryUnitTestStr = readFileAsString("documentaryUnit.json");
    }

    /**
     * CR(U)D cycle could also use curl like this: curl -v -X POST -H
     * "Authorization: 80497" -H "Accept: application/json" -H
     * "Content-type: application/json"
     * http://localhost:7474/examples/unmanaged/ehri/documentaryUnit -d
     * '{"data":{"name":"a collection","identifier":"some id",
     * "isA":"documentaryUnit"
     * },"relationships":{"describes":[{"data":{"identifier":"some id",
     * "title":"a description"
     * ,"isA":"documentDescription","languageOfDescription":"en"}}],
     * "hasDate":[{"data":{"startDate":"1940-01-01T00:00:00Z","endDate":
     * "1945-01-01T00:00:00Z", "isA":"datePeriod"}}]}}'
     * 
     * curl -v -X GET -H "Authorization: 80497" -H "Accept: application/json"
     * http://localhost:7474/examples/unmanaged/ehri/documentaryUnit/80501
     * 
     * curl -v -X DELETE -H "Authorization: 80497" -H "Accept: application/json"
     * http://localhost:7474/examples/unmanaged/ehri/documentaryUnit/80501
     */
    @Test
    public void testCreateDeleteDocumentaryUnit() throws Exception {
        // Create
        WebResource resource = client.resource(extensionEntryPointUri
                + "/documentaryUnit");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        adminUserProfileId).entity(jsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // TODO test if json is valid?
        // response.getEntity(String.class)

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        adminUserProfileId).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // TODO again test json
    }

    @Test
    public void testUpdateDocumentaryUnit() throws Exception {

        // -create data for testing
        WebResource resource = client.resource(extensionEntryPointUri
                + "/documentaryUnit");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        adminUserProfileId).entity(jsonDocumentaryUnitTestStr)
                .post(ClientResponse.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // TODO test if json is valid?
        // response.getEntity(String.class)

        // Get created doc via the response location?
        URI location = response.getLocation();

        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        adminUserProfileId).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and change it
        String json = response.getEntity(String.class);
        EntityBundle<DocumentaryUnit> entityBundle = converter
                .jsonToBundle(json);
        Map<String, Object> data = entityBundle.getData();
        entityBundle.setDataValue("name", UPDATED_NAME);
        String toUpdateJson = converter.bundleToJson(entityBundle);

        // -update
        resource = client.resource(extensionEntryPointUri + "/documentaryUnit");
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        adminUserProfileId).entity(toUpdateJson)
                .put(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and convert to a bundle, is it changed?
        resource = client.resource(location);
        response = resource
                .accept(MediaType.APPLICATION_JSON)
                .header(EhriNeo4jFramedResource.AUTH_HEADER_NAME,
                        adminUserProfileId).get(ClientResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // -get the data and convert to a bundle, is it OK?
        String updatedJson = response.getEntity(String.class);
        EntityBundle<DocumentaryUnit> updatedEntityBundle = converter
                .jsonToBundle(updatedJson);
        Map<String, Object> updatedData = updatedEntityBundle.getData();
        assertEquals(UPDATED_NAME, updatedData.get("name"));
    }

}
