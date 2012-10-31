package eu.ehri.extension.test;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import eu.ehri.extension.AbstractRestResource;
import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.base.AccessibleEntity;

public class PermissionRestClientTest extends BaseRestClientTest {

    static final String LIMITED_USER_NAME = "reto";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        initializeTestDb(PermissionRestClientTest.class.getName());
    }

    // TODO: Write sets for getting/setting permissions...
    @Test
    public void testSettingPermissions() {
        
    }
    
}
