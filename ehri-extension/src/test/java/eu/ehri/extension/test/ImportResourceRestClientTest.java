package eu.ehri.extension.test;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.importers.IcaAtomEadHandler;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import static eu.ehri.extension.ImportResource.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ImportResourceRestClientTest extends BaseRestClientTest {
    // NB: This file is *copied* into the extension test resources because
    // I can't figure out how to refer to resources in another module...
    protected static final String SINGLE_EAD = "single-ead.xml";

    @Test
    public void testImportSkos() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = ClassLoader.getSystemResourceAsStream("simple.n3");

        URI uri = ehriUriBuilder("import", "skos")
                .queryParam(LOG_PARAM, "Testing SKOS")
                .queryParam(SCOPE_PARAM, "cvoc1")
                .queryParam(TOLERANT_PARAM, "true")
                .queryParam(FORMAT_PARAM, "Turtle")
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .entity(payloadStream)
                .post(ClientResponse.class);

        assertStatus(ClientResponse.Status.OK, response);
        String output = response.getEntity(String.class);
        System.out.println("SKOS: " + output);
        JsonNode rootNode = jsonMapper.readValue(output, JsonNode.class);
        assertEquals(1, rootNode.path("created").asInt());
        assertEquals(0, rootNode.path("updated").asInt());
        assertEquals(0, rootNode.path("unchanged").asInt());
    }

    @Test
    public void testImportEad() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getPayloadStream(SINGLE_EAD);

        URI uri = getImportUrl("r1", "Testing import", false)
                .queryParam(HANDLER_PARAM, IcaAtomEadHandler.class.getName())
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .header("Content-Type", "text/plain")
                .entity(payloadStream)
                .post(ClientResponse.class);

        assertStatus(ClientResponse.Status.OK, response);
        String output = response.getEntity(String.class);

        JsonNode rootNode = jsonMapper.readValue(output, JsonNode.class);
        assertEquals(1, rootNode.path("created").asInt());
        assertEquals(0, rootNode.path("updated").asInt());
        assertEquals(0, rootNode.path("unchanged").asInt());
    }

    @Test
    public void testImportEadWithNonExistentClass() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getPayloadStream(SINGLE_EAD);

        URI uri = getImportUrl("r1", "Test", false)
                .queryParam(HANDLER_PARAM, "IDontExist") // oops
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .header("Content-Type", "text/plain")
                .entity(payloadStream)
                .post(ClientResponse.class);

        assertStatus(ClientResponse.Status.BAD_REQUEST, response);
        String output = response.getEntity(String.class);
        System.out.println(output);
        JsonNode rootNode = jsonMapper.readValue(output, JsonNode.class);
        assertTrue("Has correct error messages", rootNode.path("details").toString()
                .contains("Class not found"));
    }

    @Test
    public void testImportEadWithBadClass() throws Exception {
        // Get the path of an EAD file
        InputStream payloadStream = getPayloadStream(SINGLE_EAD);

        URI uri = getImportUrl("r1", "Test", false)
                .queryParam(HANDLER_PARAM, "java.lang.String") // oops
                .build();
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .header("Content-Type", "text/plain")
                .entity(payloadStream)
                .post(ClientResponse.class);

        assertStatus(ClientResponse.Status.BAD_REQUEST, response);
        String output = response.getEntity(String.class);
        System.out.println(output);
        JsonNode rootNode = jsonMapper.readValue(output, JsonNode.class);
        assertTrue("Has correct error messages", rootNode.path("details").toString()
                .contains("not an instance of"));
    }

    private UriBuilder getImportUrl(String scopeId, String log, boolean tolerant) {
        return ehriUriBuilder("import", "ead")
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
}
