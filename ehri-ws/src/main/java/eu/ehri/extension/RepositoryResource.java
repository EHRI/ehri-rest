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

import com.google.common.collect.Lists;
import eu.ehri.extension.base.*;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.exporters.ead.Ead2002Exporter;
import eu.ehri.project.exporters.ead.EadExporter;
import eu.ehri.project.exporters.eag.Eag2012Exporter;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.json.BatchOperations;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.Table;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.util.List;

/**
 * Provides a web service interface for the Repository.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.REPOSITORY)
public class RepositoryResource extends AbstractAccessibleResource<Repository>
        implements ParentResource, GetResource, ListResource, UpdateResource, DeleteResource {

    public RepositoryResource(@Context DatabaseManagementService service) {
        super(service, Repository.class);
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
            Repository repository = api().get(id, cls);
            Response response = streamingPage(() -> {
                Iterable<DocumentaryUnit> units = all
                        ? repository.getAllDocumentaryUnits()
                        : repository.getTopLevelDocumentaryUnits();
                return getQuery().page(units, DocumentaryUnit.class);
            });
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
            throws PermissionDenied, ItemNotFound, ValidationError, HierarchyError {
        try (final Tx tx = beginTx()) {
            deleteItem(id);
            tx.success();
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

    /**
     * Create a documentary unit for this repository.
     *
     * @param id     The repository ID
     * @param bundle The new unit data
     * @return The new unit
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
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            final Repository repository = api().get(id, cls);
            Response response = createItem(bundle, accessors,
                    repository::addTopLevelDocumentaryUnit,
                    api().withScope(repository), DocumentaryUnit.class);
            tx.success();
            return response;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/list")
    public ImportLog addChildren(
            @PathParam("id") String id,
            @QueryParam(TOLERANT_PARAM) @DefaultValue("false") boolean tolerant,
            @QueryParam(COMMIT_PARAM) @DefaultValue("false") boolean commit,
            InputStream data) throws ItemNotFound, DeserializationError, ValidationError, PermissionDenied {
        try (final Tx tx = beginTx()) {
            Actioner user = getCurrentActioner();
            Repository repository = api().get(id, cls);
            ImportCallback cb = mutation -> {
                Accessible accessible = mutation.getNode();
                if (!Entities.DOCUMENTARY_UNIT.equals(accessible.getType())) {
                    throw new RuntimeException("Bundle is not a documentary unit: " + accessible.getId());
                }
                accessible.setPermissionScope(repository);
                repository.addTopLevelDocumentaryUnit(accessible.as(DocumentaryUnit.class));
            };
            ImportLog log = new BatchOperations(graph, repository, true, tolerant,
                    Lists.newArrayList(cb)).batchImport(data, user, getLogMessage());
            if (commit) {
                logger.debug("Committing batch ingest transaction...");
                tx.success();
            }
            return log;
        }
    }

    /**
     * Export the given repository as an EAG file.
     *
     * @param id   the unit id
     * @param lang a three-letter ISO639-2 code
     * @return an EAG XML Document
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Path("{id:[^/]+}/eag")
    @Produces(MediaType.TEXT_XML)
    public Response exportEag(@PathParam("id") String id,
            final @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String lang)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            Repository repository = api().get(id, cls);
            tx.success();
            return Response.ok((StreamingOutput) outputStream -> {
                try (final Tx tx2 = beginTx()) {
                    new Eag2012Exporter(api()).export(repository, outputStream, lang);
                    tx2.success();
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
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Path("{id:[^/]+}/ead")
    @Produces("application/zip")
    public Response exportEad(@PathParam("id") String id,
            final @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String lang)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            final Repository repo = api().get(id, cls);
            final EadExporter eadExporter = new Ead2002Exporter(api());
            Response response = exportItemsAsZip(eadExporter, repo.getTopLevelDocumentaryUnits(), lang);
            tx.success();
            return response;
        }
    }
}
