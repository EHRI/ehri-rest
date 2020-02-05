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

import eu.ehri.extension.base.AbstractAccessibleResource;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.extension.base.CreateResource;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.ParentResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.exporters.eag.Eag2012Exporter;
import eu.ehri.project.exporters.eag.EagExporter;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.Repository;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.Table;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * Provides a web service interface for managing countries
 * and creating repositories within them.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.COUNTRY)
public class CountryResource
        extends AbstractAccessibleResource<Country>
        implements CreateResource, GetResource, ListResource, UpdateResource, ParentResource, DeleteResource {

    public CountryResource(@Context DatabaseManagementService service) {
        super(service, Country.class);
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
            Country country = api().get(id, cls);
            Response response = streamingPage(() -> getQuery()
                    .page(country.getRepositories(), Repository.class));
            tx.success();
            return response;
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
    public Table deleteChildren(@PathParam("id") String id, @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, PermissionDenied, ValidationError, HierarchyError {
        try (final Tx tx = beginTx()) {
            Table out = deleteContents(id, all);
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
            final Country country = api().get(id, cls);
            final EagExporter eagExporter = new Eag2012Exporter(api());
            Response response = exportItemsAsZip(eagExporter, country.getRepositories(), lang);
            tx.success();
            return response;
        }
    }
}
