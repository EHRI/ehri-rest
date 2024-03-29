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

package eu.ehri.project.ws;

import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.exporters.eag.Eag2012Exporter;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.Repository;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.Table;
import eu.ehri.project.ws.base.*;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides a web service interface for managing countries
 * and creating repositories within them.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.COUNTRY)
public class CountryResource
        extends AbstractAccessibleResource<Country>
        implements CreateResource, GetResource, ListResource, UpdateResource, ParentResource, DeleteResource {

    public CountryResource(@Context GraphDatabaseService database) {
        super(database, Country.class);
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/list")
    @Override
    public Response listChildren(@PathParam("id") String id,
                                 @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, cls);
            Response page = streamingPage(() -> getQuery()
                    .page(manager.getEntityUnchecked(id, cls).getRepositories(), Repository.class));
            tx.success();
            return page;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response create(Bundle bundle,
                           @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, DeserializationError {
        try (Tx tx = beginTx()) {
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
        try (Tx tx = beginTx()) {
            Response item = updateItem(id, bundle);
            tx.success();
            return item;
        }
    }

    @DELETE
    @Path("{id:[^/]+}/list")
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Override
    public Table deleteChildren(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all,
            @QueryParam(VERSION_PARAM) @DefaultValue("true") boolean version,
            @QueryParam("batch") @DefaultValue("-1") int batchSize)
                throws ItemNotFound, PermissionDenied, HierarchyError {
        try (final Tx tx = beginTx()) {
            Table out = deleteContents(id, all, version, batchSize);
            tx.success();
            return out;
        }
    }

    @DELETE
    @Path("{id:[^/]+}")
    @Override
    public void delete(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError, HierarchyError {
        try (Tx tx = beginTx()) {
            deleteItem(id);
            tx.success();
        }
    }

    /**
     * Create a top-level repository unit for this country.
     *
     * @param id     The country id
     * @param bundle The new repository data
     * @return The new repository
     * @throws ItemNotFound         if the parent does not exist
     * @throws PermissionDenied     if the user cannot perform the action
     * @throws DeserializationError if the input data is not well formed
     * @throws ValidationError      if data constraints are not met
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response createChild(@PathParam("id") String id,
                                @QueryParam(ACCESSOR_PARAM) List<String> accessors,
                                Bundle bundle)
            throws PermissionDenied, ValidationError, DeserializationError, ItemNotFound {
        try (Tx tx = beginTx()) {
            final Country country = api().get(id, cls);
            Response item = createItem(bundle, accessors,
                    repository -> repository.setCountry(country),
                    api().withScope(country), Repository.class);
            tx.success();
            return item;
        }
    }

    /**
     * Export the given country's repositories as EAG streamed
     * in a ZIP file.
     *
     * @param id   the country ID
     * @param lang a three-letter ISO639-2 code
     * @return a zip containing the country's repositories as EAG
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Path("{id:[^/]+}/eag")
    @Produces("application/zip")
    public Response exportEag(
            @PathParam("id") String id,
            final @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String lang)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, cls);
            Response response = streamingXmlZip(() -> new Eag2012Exporter(api()), () -> {
                final Country country = manager.getEntityUnchecked(id, cls);
                return country.getRepositories();
            }, lang);
            tx.success();
            return response;
        }
    }
}
