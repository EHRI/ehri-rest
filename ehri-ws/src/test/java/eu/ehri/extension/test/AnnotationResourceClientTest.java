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

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.extension.AnnotationResource;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.project.definitions.Entities;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

import java.net.URI;

import static com.sun.jersey.api.client.ClientResponse.Status.CREATED;

public class AnnotationResourceClientTest extends AbstractResourceClientTest {

    @Test
    public void testCreateAnnotation() throws Exception {
        // Create a link annotation between two objects
        URI uri = entityUriBuilder(Entities.ANNOTATION)
                .queryParam(AnnotationResource.TARGET_PARAM, "c1").build();
        WebResource resource = client.resource(uri);
        String jsonAnnotationTestString = "{\"type\": \"Annotation\", " +
                "\"data\":{\"identifier\": \"39dj28dhs\", \"body\": \"test\"}}";
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId()).entity(jsonAnnotationTestString)
                .post(ClientResponse.class);
        assertStatus(CREATED, response);
    }
}
