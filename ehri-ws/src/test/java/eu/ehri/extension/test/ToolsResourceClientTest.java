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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.extension.utils.Table;
import eu.ehri.project.definitions.Entities;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;

import java.util.Collections;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static eu.ehri.extension.ToolsResource.ENDPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test web service miscellaneous functions.
 */
public class ToolsResourceClientTest extends AbstractResourceClientTest {
    @Test
    public void testFindReplace() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "find-replace"))
                .queryParam("type", Entities.REPOSITORY)
                .queryParam("subtype", Entities.REPOSITORY_DESCRIPTION)
                .queryParam("property", "name")
                .queryParam("commit", "true");
        MultivaluedMap<String, String> data = new MultivaluedMapImpl();
        data.putSingle("from", "KCL Description");
        data.putSingle("to", "NEW VALUE");
        ClientResponse response = resource
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .header("Accept", "text/csv")
                .header(AbstractResource.LOG_MESSAGE_HEADER_NAME,
                        "This is a test!")
                .post(ClientResponse.class, data);
        Table out = response.getEntity(Table.class);
        assertStatus(OK, response);
        assertEquals(1, out.data().size());
        assertEquals(Lists.newArrayList("r2", "rd2", "KCL Description"), out.data().get(0));
    }

    @Test
    public void testRegenerateIds() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "regenerate-ids"))
                .queryParam("id", "c1")
                .queryParam("commit", "true");
        Table data = Table.of(Collections.singletonList(Collections.singletonList("c4")));
        ClientResponse response = resource.post(ClientResponse.class, data);
        Table out = response.getEntity(Table.class);
        assertStatus(OK, response);
        assertEquals(2, out.data().size());
        assertTrue(out.contains("c1", "nl-r1-c1"));
        assertTrue(out.contains("c4", "nl-r1-c4"));
    }

    @Test
    public void testRegenerateIdsForScope() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "regenerate-ids-for-scope", "r1"))
                .queryParam("commit", "true");
        ClientResponse response = resource.post(ClientResponse.class);
        Table out = response.getEntity(Table.class);
        assertStatus(OK, response);
        assertEquals(4, out.data().size());
        assertTrue(out.contains("c1", "nl-r1-c1"));
        assertTrue(out.contains("c2", "nl-r1-c1-c2"));
        assertTrue(out.contains("c3", "nl-r1-c1-c2-c3"));
        assertTrue(out.contains("c4", "nl-r1-c4"));
    }

    @Test
    public void testRegenerateIdsForType() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "regenerate-ids-for-type",
                Entities.DOCUMENTARY_UNIT)).queryParam("commit", "true");
        ClientResponse response = resource.post(ClientResponse.class);
        Table out = response.getEntity(Table.class);
        assertStatus(OK, response);
        assertEquals(4, out.data().size());
        assertTrue(out.contains("c1", "nl-r1-c1"));
        assertTrue(out.contains("c2", "nl-r1-c1-c2"));
        assertTrue(out.contains("c3", "nl-r1-c1-c2-c3"));
        assertTrue(out.contains("c4", "nl-r1-c4"));
    }

    @Test
    public void testRegenerateDescriptionIds() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "regenerate-description-ids"))
                .queryParam("commit", "true");
        ClientResponse response = resource.post(ClientResponse.class);
        String out = response.getEntity(String.class);
        assertStatus(OK, response);
        // 2 Historical agent descriptions
        // 4 Repository descriptions
        // 6 Doc Unit descriptions (total 7, 1 is okay in fixtures)
        // 2 Concept descriptions
        assertEquals("14", out);
    }

    @Test
    public void testRelinking() throws Exception {
        WebResource resource = client.resource(ehriUri(ENDPOINT, "relink-targets"));
        ClientResponse response = resource
                .accept("text/csv")
                .post(ClientResponse.class,
                        Table.of(ImmutableList.of(ImmutableList.of("a1", "a2"))));
        String out = response.getEntity(String.class);
        assertStatus(OK, response);
        assertEquals("a1,a2,2\n", out);
    }
}
