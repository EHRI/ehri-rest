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

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import eu.ehri.project.ws.BatchResource;
import eu.ehri.project.ws.GenericResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.Table;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

import static eu.ehri.project.ws.BatchResource.COMMIT_PARAM;
import static eu.ehri.project.ws.BatchResource.LOG_PARAM;
import static eu.ehri.project.ws.BatchResource.SCOPE_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class BatchResourceClientTest extends AbstractResourceClientTest {

    @Test
    public void testBatchUpdate() throws Exception {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-patch-test.json");
        System.out.println(payloadStream);
        String logText = "Testing patch update";
        URI jsonUri = ehriUriBuilder(BatchResource.ENDPOINT, "update")
                .queryParam(COMMIT_PARAM, true)
                .queryParam(LOG_PARAM, logText).build();
        Response response = callAs(getAdminUserProfileId(), jsonUri)
                .put(Entity.json(payloadStream), Response.class);

        ImportLog log = response.readEntity(ImportLog.class);
        assertEquals(0, log.getCreated());
        assertEquals(1, log.getUpdated());
        assertEquals(1, log.getUnchanged());
        assertEquals(logText, log.getLogMessage().orElse(null));
    }

    @Test
    public void testBatchUpdateToUnsetValues() throws Exception {
        Bundle before = getEntity(Entities.REPOSITORY, "r1",
                getAdminUserProfileId());
        assertEquals("Repository 1", before.getDataValue("name"));
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-patch-test2.json");
        String logText = "Testing patch to unset values";
        URI jsonUri = ehriUriBuilder(BatchResource.ENDPOINT, "update")
                .queryParam(LOG_PARAM, logText)
                .queryParam(COMMIT_PARAM, true)
                .queryParam(SCOPE_PARAM, "nl").build();
        Response response = callAs(getAdminUserProfileId(), jsonUri)
                .put(Entity.json(payloadStream), Response.class);

        assertStatus(Response.Status.OK, response);

        Bundle after = getEntity(Entities.REPOSITORY, "r1",
                getAdminUserProfileId());
        assertNull(after.getDataValue("name"));
    }

    @Test
    public void testBatchDelete() throws Exception {
        String user = getAdminUserProfileId();
        assertTrue(checkExists("a1", user));
        assertTrue(checkExists("a2", user));
        String logText = "Testing patch delete";
        Table table = Table.of(ImmutableList.of(
                ImmutableList.of("a1"),
                ImmutableList.of("a2")));
        URI jsonUri = ehriUriBuilder(BatchResource.ENDPOINT, "delete")
                .queryParam(LOG_PARAM, logText)
                .queryParam(COMMIT_PARAM, true)
                .build();
        Response response = callAs(user, jsonUri)
                .post(Entity.json(table), Response.class);
        assertStatus(Response.Status.OK, response);
        assertEquals("2", response.readEntity(String.class));
        assertFalse(checkExists("a1", user));
        assertFalse(checkExists("a2", user));
    }

    @Test
    public void testBatchDeleteWithMissing() throws Exception {
        String user = getAdminUserProfileId();
        String logText = "Testing patch delete";
        Table table = Table.of(Collections.singletonList(Lists.newArrayList("missing")));
        URI jsonUri = ehriUriBuilder(BatchResource.ENDPOINT, "delete")
                .queryParam(LOG_PARAM, logText)
                .queryParam(COMMIT_PARAM, true)
                .build();
        Response response = callAs(user, jsonUri)
                .post(Entity.entity(table, MediaType.APPLICATION_JSON_TYPE), Response.class);
        assertStatus(Response.Status.BAD_REQUEST, response);
    }


    private boolean checkExists(String id, String userId) {
        URI uri = ehriUriBuilder(GenericResource.ENDPOINT, id).build();
        return callAs(userId, uri).get(Response.class).getStatus()
                == Response.Status.OK.getStatusCode();
    }
}
