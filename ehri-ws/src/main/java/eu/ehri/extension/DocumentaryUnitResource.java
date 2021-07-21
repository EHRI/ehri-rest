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
import eu.ehri.extension.errors.ConflictError;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.exporters.ead.Ead2002Exporter;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.tools.IdRegenerator;
import eu.ehri.project.utils.Table;
import org.neo4j.dbms.api.DatabaseManagementService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.List;

/**
 * Provides a web service interface for the DocumentaryUnit model.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.DOCUMENTARY_UNIT)
public class DocumentaryUnitResource
        extends AbstractAccessibleResource<DocumentaryUnit>
        implements GetResource, ListResource, UpdateResource, ParentResource, DeleteResource {

    public DocumentaryUnitResource(@Context DatabaseManagementService service) {
        super(service, DocumentaryUnit.class);
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
            Response response = streamingPage(() -> {
                DocumentaryUnit parent = manager.getEntityUnchecked(id, DocumentaryUnit.class);
                Iterable<DocumentaryUnit> units = all
                        ? parent.getAllChildren()
                        : parent.getChildren();
                return getQuery().page(units, cls);
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
    public Response update(@PathParam("id") String id,
            Bundle bundle) throws PermissionDenied,
            ValidationError, DeserializationError, ItemNotFound {
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

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({MediaType.APPLICATION_JSON, CSV_MEDIA_TYPE})
    @Path("{id:[^/]+}/rename")
    public Table rename(
            @PathParam("id") String id,
            @QueryParam("check") @DefaultValue("false") boolean check,
            String newIdentifier)
            throws PermissionDenied, ItemNotFound, ValidationError, SerializationError,
            DeserializationError, ConflictError {
        try (final Tx tx = beginTx()) {
            IdRegenerator idGen = new IdRegenerator(graph)
                    .withActualRename(!check)
                    .collisionMode(check);
            DocumentaryUnit entity = api().get(id, DocumentaryUnit.class);
            Bundle newBundle = getSerializer()
                    .withDependentOnly(true)
                    .entityToBundle(entity)
                    .withDataValue(Ontology.IDENTIFIER_KEY, newIdentifier);
            api().update(newBundle, DocumentaryUnit.class, getLogMessage());
            List<DocumentaryUnit> todo = Lists.newArrayList(entity);
            for (DocumentaryUnit child : entity.getAllChildren()) {
                todo.add(child);
            }
            List<List<String>> done = idGen.reGenerateIds(todo);
            tx.success();
            return Table.of(done);
        } catch (IdRegenerator.IdCollisionError e) {
            throw new ConflictError(e);
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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response createChild(@PathParam("id") String id,
                                @QueryParam(ACCESSOR_PARAM) List<String> accessors,
                                Bundle bundle)
            throws PermissionDenied, ValidationError, DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            final DocumentaryUnit parent = api().get(id, cls);
            Response resource = createItem(bundle, accessors,
                    parent::addChild,
                    api().withScope(parent), cls);
            tx.success();
            return resource;
        }
    }

    /**
     * Export the given documentary unit as EAD.
     *
     * @param id   the unit id
     * @param lang a three-letter ISO639-2 code
     * @return an EAD XML Document
     * @throws ItemNotFound if the item does not exist
     */
    @GET
    @Path("{id:[^/]+}/ead")
    @Produces(MediaType.TEXT_XML)
    public Response exportEad(@PathParam("id") String id,
            final @QueryParam(LANG_PARAM) @DefaultValue(DEFAULT_LANG) String lang)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            DocumentaryUnit unit = api().get(id, cls);
            tx.success();
            return Response.ok((StreamingOutput) outputStream -> {
                try (final Tx tx2 = beginTx()) {
                    new Ead2002Exporter(api()).export(unit, outputStream, lang);
                    tx2.success();
                }
            }).type(MediaType.TEXT_XML + "; charset=utf-8").build();
        }
    }
}
