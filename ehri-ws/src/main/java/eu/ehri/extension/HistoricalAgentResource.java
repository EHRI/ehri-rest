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

package eu.ehri.extension;

import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.extension.base.AbstractRestResource;
import eu.ehri.extension.base.CreateResource;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.exporters.DocumentWriter;
import eu.ehri.project.exporters.eac.Eac2010Exporter;
import eu.ehri.project.exporters.eac.EacExporter;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.persistence.Bundle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.w3c.dom.Document;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.List;

/**
 * Provides a web service interface for the HistoricalAgent model.
 */
@Path(AbstractRestResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.HISTORICAL_AGENT)
public class HistoricalAgentResource extends AbstractAccessibleResource<HistoricalAgent>
        implements GetResource, ListResource, CreateResource, UpdateResource, DeleteResource {

    public HistoricalAgentResource(@Context GraphDatabaseService database) {
        super(database, HistoricalAgent.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        return getItem(id);

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response list() {
        return listItems();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response create(Bundle bundle,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, DeserializationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response item = createItem(bundle, accessors);
            tx.success();
            return item;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response response = updateItem(id, bundle);
            tx.success();
            return response;
        }
    }

    @DELETE
    @Path("{id:[^/]+}")
    @Override
    public void delete(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            deleteItem(id);
            tx.success();
        }
    }

    /**
     * Export the given historical agent as an EAC document.
     *
     * @param id   the unit id
     * @param lang a three-letter ISO639-2 code
     * @return an EAD XML Document
     * @throws IOException
     * @throws ItemNotFound
     */
    @GET
    @Path("{id:[^/]+}/eac")
    @Produces(MediaType.TEXT_XML)
    public Response exportEac(@PathParam("id") String id,
            final @QueryParam("lang") @DefaultValue("eng") String lang)
            throws IOException, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            HistoricalAgent agent = api().detail(id, HistoricalAgent.class);
            EacExporter eacExporter = new Eac2010Exporter(graph, api());
            final Document document = eacExporter.export(agent, lang);
            tx.success();
            return Response.ok((StreamingOutput) outputStream -> {
                try {
                    new DocumentWriter(document).write(outputStream);
                } catch (TransformerException e) {
                    throw new WebApplicationException(e);
                }
            }).type(MediaType.TEXT_XML + "; charset=utf-8").build();
        }
    }
}
