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

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.definitions.Entities;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.zip.ZipEntry;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;

public class AuthoritativeSetResourceClientTest extends AbstractResourceClientTest {
    @Test
    public void testExportEac() throws Exception {
        // Create
        URI uri = entityUri(Entities.AUTHORITATIVE_SET, "auths", "eac");
        ClientResponse response = callAs(getAdminUserProfileId(), uri)
                .get(ClientResponse.class);
        assertStatus(OK, response);
        try (InputStream stream = response.getEntityInputStream()) {
            List<ZipEntry> entries = readZip(stream);
            assertEquals(2, entries.size());
        }
    }
}
