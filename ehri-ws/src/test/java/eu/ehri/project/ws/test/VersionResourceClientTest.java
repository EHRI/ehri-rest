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

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.ws.GenericResource;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.persistence.Bundle;
import org.junit.Test;

import java.util.List;

import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VersionResourceClientTest extends AbstractResourceClientTest {

    @Test
    public void testGetVersionsForItem() throws Exception {
        // Create an item
        Bundle before = getEntity(Entities.REPOSITORY, "r1", getAdminUserProfileId());
        String jsonAgentTestString = "{\"type\": \"Repository\", \"data\":{\"identifier\": \"jmp\"}}";
        ClientResponse response = jsonCallAs(getAdminUserProfileId(),
                entityUri(Entities.REPOSITORY, "r1"))
                .entity(jsonAgentTestString)
                .put(ClientResponse.class);
        assertStatus(OK, response);

        List<Bundle> versions = getItemList(ehriUri(GenericResource.ENDPOINT, "r1", "versions"),
                getAdminUserProfileId());
        assertEquals(1, versions.size());
        String data = versions.get(0).getDataValue(Ontology.VERSION_ENTITY_DATA);
        assertNotNull(data);
        assertEquals(before.getData(), Bundle.fromString(data).getData());
    }
}
