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

import com.google.common.collect.Lists;
import eu.ehri.project.api.Api;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.exporters.ead.Ead2002Exporter;
import eu.ehri.project.exporters.ead.Ead3Exporter;
import eu.ehri.project.exporters.eag.Eag2012Exporter;
import eu.ehri.project.exporters.xml.XmlExporter;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.json.BatchOperations;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.Table;
import eu.ehri.project.ws.base.*;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

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
            checkExists(id, cls);
            Response page = streamingPage(() -> {
                Repository repository = manager.getEntityUnchecked(id, cls);
                Iterable<DocumentaryUnit> units = all
                        ? repository.getAllDocumentaryUnits()
                        : repository.getTopLevelDocumentaryUnits();
                return getQuery().page(units, DocumentaryUnit.class);
            });
            tx.success();
            return page;
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

    /**
     * Import a set of top-level documentary units to a repository.
     *
     * @param id       the repository ID
     * @param tolerant whether to accept individual validation errors
     * @param commit   actually commit changes
     * @param data     a list of repository bundles
     * @return an import log
     * @throws ItemNotFound         if the repository is not found
     * @throws DeserializationError if the data is malformed
     * @throws ValidationError      if any items do not validate
     * @throws PermissionDenied     if the user is unable to
     *                              perform the action
     */
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
            checkExists(id, cls);
            tx.success();
            return Response.ok((StreamingOutput) outputStream -> {
                try (final Tx tx2 = beginTx()) {
                    Repository repository = manager.getEntityUnchecked(id, cls);
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
    @Path("{id:[^/]+}/{fmt:ead3?}")
    @Produces("application/zip")
    public Response exportEad(
            final @PathParam("id") String id,
            final @PathParam("fmt") @DefaultValue("ead") String fmt,
            final @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String lang)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, cls);
            final Supplier<XmlExporter<DocumentaryUnit>> exporter = () -> {
                final Api api = api();
                return fmt.equals("ead")
                        ? new Ead2002Exporter(api)
                        : new Ead3Exporter(api);
            };
            Response response = streamingXmlZip(exporter, () -> {
                final Repository repo = manager.getEntityUnchecked(id, cls);
                return repo.getTopLevelDocumentaryUnits();
            }, lang);
            tx.success();
            return response;
        }
    }
}
