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

import eu.ehri.extension.base.*;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.exporters.eac.Eac2010Exporter;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.persistence.Bundle;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.List;

/**
 * Provides a web service interface for the HistoricalAgent model.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.HISTORICAL_AGENT)
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
        try (final Tx tx = beginTx()) {
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
        try (final Tx tx = beginTx()) {
            Response response = updateItem(id, bundle);
            tx.success();
            return response;
        }
    }

    @DELETE
    @Path("{id:[^/]+}")
    @Override
    public void delete(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError, HierarchyError {
        try (final Tx tx = beginTx()) {
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
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Path("{id:[^/]+}/eac")
    @Produces(MediaType.TEXT_XML)
    public Response exportEac(@PathParam("id") String id,
                              final @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String lang)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, cls);
            tx.success();
            return Response.ok((StreamingOutput) outputStream -> {
                try (final Tx tx2 = beginTx()) {
                    HistoricalAgent agent = manager.getEntityUnchecked(id, HistoricalAgent.class);
                    new Eac2010Exporter(api()).export(agent, outputStream, lang);
                    tx2.success();
                }
            }).type(MediaType.TEXT_XML + "; charset=utf-8").build();
        }
    }
}
