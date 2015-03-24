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
import com.sun.jersey.core.util.MultivaluedMapImpl;
import eu.ehri.extension.AbstractRestResource;
import eu.ehri.extension.AccessResource;
import eu.ehri.project.definitions.Entities;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;


public class AccessRestClientTest extends BaseRestClientTest {

    static final String PRIVILEGED_USER_NAME = "mike";
    static final String LIMITED_USER_NAME = "reto";

    @Test @Ignore
    public void testUserCannotRead() throws Exception {
        // Create
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

    @Test
    public void testGrantAccess() throws Exception {
        // Attempt to fetch an element.
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);

        assertStatus(NOT_FOUND, response);

        // Set the form data
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add(AbstractRestResource.ACCESSOR_PARAM, LIMITED_USER_NAME);

        response = client.resource(ehriUri(AccessResource.ENDPOINT  , "c1"))
                .queryParams(queryParams)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);
        assertStatus(OK, response);

        // Try the original request again and ensure it worked...
        response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);
        assertStatus(OK, response);
    }

    @Test
    public void testRevokeAccess() throws Exception {

        // first, grant access
        testGrantAccess();

        // Create
        ClientResponse response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);

        assertStatus(OK, response);

        // Set the form data
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add(AbstractRestResource.ACCESSOR_PARAM, PRIVILEGED_USER_NAME);

        WebResource resource = client.resource(ehriUri(AccessResource.ENDPOINT, "c1"));
        response = resource
                .queryParams(queryParams)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .header(AbstractRestResource.AUTH_HEADER_NAME,
                        getAdminUserProfileId())
                .post(ClientResponse.class);
        assertStatus(OK, response);

        // Try the original request again and ensure it worked...
        response = jsonCallAs(LIMITED_USER_NAME,
                ehriUri(Entities.DOCUMENTARY_UNIT, "c1")).get(ClientResponse.class);
        assertStatus(NOT_FOUND, response);
    }

}
