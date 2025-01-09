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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.utils.Table;
import eu.ehri.project.ws.ToolsResource;
import eu.ehri.project.ws.base.AbstractResource;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static eu.ehri.project.ws.base.AbstractResource.CSV_MEDIA_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test web service miscellaneous functions.
 */
public class ToolsResourceClientTest extends AbstractResourceClientTest {


    @Test
    public void testVersion() throws Exception {
        // NB: This relies on the Maven build information which is not available
        // when running tests, hence the version returned is null/204.
        WebTarget resource = client.target(ehriUri(ToolsResource.ENDPOINT, "version"));
        Response response = resource.request().get(Response.class);
        assertEquals(204, response.getStatus());
        assertEquals("", response.readEntity(String.class));
    }

    @Test
    public void testFindReplace() throws Exception {
        WebTarget resource = client.target(ehriUri(ToolsResource.ENDPOINT, "find-replace"))
                .queryParam("type", Entities.REPOSITORY)
                .queryParam("subtype", Entities.REPOSITORY_DESCRIPTION)
                .queryParam("property", "name")
                .queryParam("commit", "true");
        MultivaluedMap<String, String> data = new MultivaluedHashMap<>();
        data.putSingle("from", "KCL Description");
        data.putSingle("to", "NEW VALUE");
        Entity<Form> entity = Entity.form(data);
        Response response = resource
                .request(CSV_MEDIA_TYPE)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .header(AbstractResource.LOG_MESSAGE_HEADER_NAME,
                        "This is a test!")
                .post(entity, Response.class);
        Table out = response.readEntity(Table.class);
        assertStatus(OK, response);
        assertEquals(1, out.rows().size());
        assertEquals(Lists.newArrayList("r2", "rd2", "KCL Description"), out.rows().get(0));
    }

    @Test
    public void testRegenerateIds() throws Exception {
        WebTarget resource = client.target(ehriUri(ToolsResource.ENDPOINT, "regenerate-ids"))
                .queryParam("id", "c1")
                .queryParam("commit", "true");
        Table data = Table.of(Collections.singletonList(Collections.singletonList("c4")));
        Response response = resource.request().post(Entity.json(data), Response.class);
        Table out = response.readEntity(Table.class);
        assertStatus(OK, response);
        assertEquals(2, out.rows().size());
        assertTrue(out.contains("c1", "nl-r1-c1"));
        assertTrue(out.contains("c4", "nl-r1-c4"));
    }

    @Test
    public void testRegenerateIdsForScope() throws Exception {
        WebTarget resource = client.target(ehriUri(ToolsResource.ENDPOINT, "regenerate-ids-for-scope", "r1"))
                .queryParam("commit", "true");
        Response response = resource.request().post(Entity.json(""), Response.class);
        Table out = response.readEntity(Table.class);
        assertStatus(OK, response);
        assertEquals(4, out.rows().size());
        assertTrue(out.contains("c1", "nl-r1-c1"));
        assertTrue(out.contains("c2", "nl-r1-c1-c2"));
        assertTrue(out.contains("c3", "nl-r1-c1-c2-c3"));
        assertTrue(out.contains("c4", "nl-r1-c4"));
    }

    @Test
    public void testRegenerateIdsForType() throws Exception {
        WebTarget resource = client.target(ehriUri(ToolsResource.ENDPOINT, "regenerate-ids-for-type",
                Entities.DOCUMENTARY_UNIT)).queryParam("commit", "true");
        Response response = resource.request().post(Entity.json(""), Response.class);
        Table out = response.readEntity(Table.class);
        assertStatus(OK, response);
        assertEquals(4, out.rows().size());
        assertTrue(out.contains("c1", "nl-r1-c1"));
        assertTrue(out.contains("c2", "nl-r1-c1-c2"));
        assertTrue(out.contains("c3", "nl-r1-c1-c2-c3"));
        assertTrue(out.contains("c4", "nl-r1-c4"));
    }

    @Test
    public void testRegenerateDescriptionIds() throws Exception {
        WebTarget resource = client.target(ehriUri(ToolsResource.ENDPOINT, "regenerate-description-ids"))
                .queryParam("commit", "true");
        Response response = resource.request().post(Entity.json(""), Response.class);
        String out = response.readEntity(String.class);
        assertStatus(OK, response);
        // 2 Historical agent descriptions
        // 4 Repository descriptions
        // 6 Doc Unit descriptions (total 7, 1 is okay in fixtures)
        // 2 Concept descriptions
        assertEquals("14", out);
    }

    @Test
    public void testRelinking() throws Exception {
        WebTarget resource = client.target(ehriUri(ToolsResource.ENDPOINT, "relink-targets"));
        Response response = resource
                .request("text/csv")
                .post(Entity.json(Table.of(ImmutableList.of(ImmutableList.of("a1", "a2")))), Response.class);
        String out = response.readEntity(String.class);
        assertStatus(OK, response);
        assertEquals("a1,a2,1\n", out);
    }

    @Test
    public void testRename() throws Exception {
        WebTarget resource = client.target(ehriUri(ToolsResource.ENDPOINT, "rename"));
        Response response = resource
                .request("text/csv")
                .post(Entity.json(
                        Table.of(ImmutableList.of(
                                // Data ordered child-first to
                                // text lexical re-ordering and
                                // correct hierarchical ID generation
                                ImmutableList.of("c3", "test2"),
                                ImmutableList.of("c2", "test1")))), Response.class);
        Table out = response.readEntity(Table.class);
        assertStatus(OK, response);
        assertEquals(ImmutableList.of(
                ImmutableList.of("c2", "nl-r1-c1-test1"),
                ImmutableList.of("c3", "nl-r1-c1-test1-test2")), out.rows());
    }


    @Test
    public void testReparent() throws Exception {
        WebTarget resource = client.target(ehriUri(ToolsResource.ENDPOINT, "reparent"));
        Response response = resource
                .request("text/csv")
                .post(
                        Entity.json(Table.of(ImmutableList.of(
                                ImmutableList.of("c4", "c1"),
                                ImmutableList.of("c3", "c1")))), Response.class);
        Table out = response.readEntity(Table.class);
        assertStatus(OK, response);
        assertEquals(ImmutableList.of(
                ImmutableList.of("c4", "nl-r1-c1-c4"),
                ImmutableList.of("c3", "nl-r1-c1-c3")), out.rows());
    }
}
