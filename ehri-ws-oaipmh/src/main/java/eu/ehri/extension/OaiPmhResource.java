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

package eu.ehri.extension;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.project.api.Api;
import eu.ehri.project.core.Tx;
import eu.ehri.project.exporters.xml.IndentingXMLStreamWriter;
import eu.ehri.project.oaipmh.OaiPmhData;
import eu.ehri.project.oaipmh.OaiPmhExporter;
import eu.ehri.project.oaipmh.OaiPmhRenderer;
import eu.ehri.project.oaipmh.OaiPmhState;
import eu.ehri.project.oaipmh.errors.OaiPmhError;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.BufferedOutputStream;
import java.util.List;


/**
 * Open Archives Initiative Protocol for Metadata Harvesting
 * (OAI-PMH) 2.0 server implementation.
 */
@Path(OaiPmhResource.ENDPOINT)
public class OaiPmhResource extends AbstractResource {

    private static final Config config = ConfigFactory.load();
    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();

    public static final String ENDPOINT = "oaipmh";
    public static final String LIMIT_HEADER_NAME = "X-Limit";



    public OaiPmhResource(@Context DatabaseManagementService service) {
        super(service);
    }

    /**
     * OAI-PMH 2.0 base URL. See specification for usage.
     * <p>
     * http://www.openarchives.org/OAI/openarchivesprotocol.html
     *
     * @return an OAI-PMH XML payload as a chunked response.
     */
    @GET
    @Produces(MediaType.TEXT_XML)
    public Response oaiGet() {
        final int limit = isStreaming() ? -1 : limit(config.getInt("oaipmh.numResponses"));
        return Response.ok((StreamingOutput) out -> {
            try (final Tx tx = beginTx();
                 final BufferedOutputStream bufferedOut = new BufferedOutputStream(out);
                 final IndentingXMLStreamWriter sw = new IndentingXMLStreamWriter(
                         xmlOutputFactory.createXMLStreamWriter(bufferedOut))) {
                Api api = anonymousApi();
                OaiPmhExporter oaiPmh = new OaiPmhExporter(
                        OaiPmhData.create(api),
                        OaiPmhRenderer.defaultRenderer(api, DEFAULT_LANG),
                        config);
                try {
                    OaiPmhState state = OaiPmhState.parse(uriInfo.getRequestUri().getQuery(), limit);
                    oaiPmh.performVerb(sw, state);
                    tx.success();
                } catch (OaiPmhError error) {
                    oaiPmh.error(sw, error.getCode(), error.getMessage(), null);
                }
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }).header(HttpHeaders.CONTENT_TYPE,
                MediaType.TEXT_XML + "; charset=utf-8")
                .build();
    }

    @POST
    @Produces(MediaType.TEXT_XML)
    public Response oaiPost() {
        return oaiGet();
    }

    private int limit(int defaultLimit) {
        // Allow overriding the limit via a header. This is safe since
        // we stream requests. It's also very handy for testing.
        List<String> limit = requestHeaders.getRequestHeader(LIMIT_HEADER_NAME);
        if (limit == null || limit.size() < 1) {
            return defaultLimit;
        } else {
            try {
                return Integer.parseInt(limit.get(0));
            } catch (NumberFormatException e) {
                return defaultLimit;
            }
        }
    }
}
