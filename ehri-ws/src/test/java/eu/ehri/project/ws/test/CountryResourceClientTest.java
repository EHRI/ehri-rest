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

import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.utils.Table;
import eu.ehri.project.ws.base.AbstractResource;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;

public class CountryResourceClientTest extends AbstractResourceClientTest {
    @Test
    public void testExportEad() throws Exception {
        // Create
        URI uri = entityUri(Entities.COUNTRY, "nl", "eag");
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
        assertStatus(OK, response);
        try (InputStream stream = response.getEntityInputStream()) {
            // There should be two items: r1 and r3
            assertEquals(2, readZip(stream).size());
        }
    }

    @Test
    public void testDeleteAll() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUriBuilder(Entities.COUNTRY, "nl", "list")
                    .queryParam(AbstractResource.ALL_PARAM, "true")
                    .build())
                .delete(ClientResponse.class);
        assertStatus(OK, response);

        Table expected = Table.of(Lists.newArrayList(
                Lists.newArrayList("c1"),
                Lists.newArrayList("c2"),
                Lists.newArrayList("c3"),
                Lists.newArrayList("c4"),
                Lists.newArrayList("nl-r1-m19"),
                Lists.newArrayList("r1"),
                Lists.newArrayList("r3")
        ));
        assertEquals(expected, response.getEntity(Table.class));
    }
}
