/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie Van Wetenschappen), King's College London,
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
import eu.ehri.extension.OaiPmhResource;
import eu.ehri.project.oaipmh.Verb;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.ws.rs.core.MediaType;
import java.net.URI;

import static eu.ehri.project.test.XmlTestHelpers.assertXPath;
import static eu.ehri.project.test.XmlTestHelpers.parseDocument;

public class OaiPmhResourceClientTest extends AbstractResourceClientTest {

    @Test
    public void testIdentify() throws Exception {
        Document document = get("verb=" + Verb.Identify);
        assertXPath(document, "2.0", "/OAI-PMH/Identify/protocolVersion");
    }

    @Test
    public void testListSetsWithLimit() throws Exception {
        // With the stream header the limit will be overridden
        Document document = get("verb=" + Verb.ListSets, 1);
        assertXPath(document, "nl", "/OAI-PMH/ListSets/set[1]/setSpec");
        assertXPath(document, "", "/OAI-PMH/ListSets/set[2]/setSpec");
    }

    @Test
    public void testListSetsWithStreaming() throws Exception {
        // With the stream header the limit will be overridden
        String xml = callAs(getRegularUserProfileId(), getUri("verb=" + Verb.ListSets))
                .accept(MediaType.TEXT_XML_TYPE)
                .header(OaiPmhResource.LIMIT_HEADER_NAME, 1)
                .header(OaiPmhResource.STREAM_HEADER_NAME, "true")
                .get(String.class);
        System.out.println(xml);
        Document document = parseDocument(xml);
        assertXPath(document, "nl", "/OAI-PMH/ListSets/set[1]/setSpec");
        assertXPath(document, "nl:r1", "/OAI-PMH/ListSets/set[2]/setSpec");
    }

    // Helpers

    private Document get(String params) throws Exception {
        return get(params, 10);
    }

    private Document get(String params, int limit) throws Exception {
        URI queryUri = getUri(params);
        ClientResponse response = callAs(getRegularUserProfileId(), queryUri)
                .accept(MediaType.TEXT_XML_TYPE)
                .header(OaiPmhResource.LIMIT_HEADER_NAME, limit)
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
//        System.out.println(entity);
        return parseDocument(entity);
    }

    private URI getUri(String params) {
        return URI.create(
                ehriUriBuilder(OaiPmhResource.ENDPOINT).build().toString() + "?" + params);
    }
}
