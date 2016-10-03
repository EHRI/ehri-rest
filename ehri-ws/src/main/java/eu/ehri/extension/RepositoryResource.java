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
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.ParentResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.exporters.ead.Ead2002Exporter;
import eu.ehri.project.exporters.ead.EadExporter;
import eu.ehri.project.exporters.eag.Eag2012Exporter;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.persistence.Bundle;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.List;

/**
 * Provides a web service interface for the Repository.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.REPOSITORY)
public class RepositoryResource extends AbstractAccessibleResource<Repository>
        implements ParentResource, GetResource, ListResource, UpdateResource, DeleteResource {

    public RepositoryResource(@Context GraphDatabaseService database) {
        super(database, Repository.class);
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
    public Response listChildren(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Repository repository = api().detail(id, cls);
            Iterable<DocumentaryUnit> units = all
                    ? repository.getAllDocumentaryUnits()
                    : repository.getTopLevelDocumentaryUnits();
            Response response = streamingPage(getQuery().page(units, DocumentaryUnit.class));
            tx.success();
            return response;
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
            throws PermissionDenied, ItemNotFound, ValidationError {
        try (final Tx tx = beginTx()) {
            deleteItem(id);
            tx.success();
        }
    }

    /**
     * Create a documentary unit for this repository.
     *
     * @param id     The repository ID
     * @param bundle The new unit data
     * @return The new unit
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response createChild(@PathParam("id") String id,
            Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            final Repository repository = api().detail(id, cls);
            Response response = createItem(bundle, accessors,
                    repository::addTopLevelDocumentaryUnit,
                    api().withScope(repository), DocumentaryUnit.class);
            tx.success();
            return response;
        }
    }

    /**
     * Export the given repository as an EAG file.
     *
     * @param id   the unit id
     * @param lang a three-letter ISO639-2 code
     * @return an EAG XML Document
     */
    @GET
    @Path("{id:[^/]+}/eag")
    @Produces(MediaType.TEXT_XML)
    public Response exportEag(@PathParam("id") String id,
            final @QueryParam("lang") @DefaultValue("eng") String lang)
            throws IOException, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Repository repository = api().detail(id, cls);
            tx.success();
            return Response.ok((StreamingOutput) outputStream -> {
                try (final Tx tx2 = beginTx()) {
                    new Eag2012Exporter(api()).export(repository, outputStream, lang);
                    tx2.success();
                } catch (TransformerException e) {
                    throw new WebApplicationException(e);
                }
            }).type(MediaType.TEXT_XML + "; charset=utf-8").build();
        }
    }

    /**
     * Export the given repository's top-level units as EAD streamed
     * in a ZIP file.
     *
     * @param id   the unit id
     * @param lang a three-letter ISO639-2 code
     * @return an EAD XML Document
     */
    @GET
    @Path("{id:[^/]+}/ead")
    @Produces("application/zip")
    public Response exportEad(@PathParam("id") String id,
            final @QueryParam("lang") @DefaultValue("eng") String lang)
            throws IOException, ItemNotFound {
        try (final Tx tx = beginTx()) {
            final Repository repo = api().detail(id, cls);
            final EadExporter eadExporter = new Ead2002Exporter(api());
            Response response = exportItemsAsZip(eadExporter, repo.getTopLevelDocumentaryUnits(), lang);
            tx.success();
            return response;
        }
    }
}
