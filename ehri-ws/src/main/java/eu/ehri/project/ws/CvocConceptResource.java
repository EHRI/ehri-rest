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

import eu.ehri.project.ws.base.*;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.Table;
import eu.ehri.project.ws.base.*;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides a web service interface for the Concept model. Note that the concept
 * creation endpoint is part of the VocabularyResource and creation without a
 * Vocabulary is not possible via this API.
 */
@Path(AbstractResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.CVOC_CONCEPT)
public class CvocConceptResource extends AbstractAccessibleResource<Concept>
        implements ParentResource, GetResource, ListResource, UpdateResource, DeleteResource, ChildResource {

    public CvocConceptResource(@Context GraphDatabaseService database) {
        super(database, Concept.class);
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

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = updateItem(id, bundle);
            tx.success();
            return item;
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
    @Path("{id:[^/]+}/all")
    @Override
    public Table deleteChildren(@PathParam("id") String id, boolean all)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try {
            // NB: While it has hierarchical behaviour this does not
            // extend to full ownership so this action is essentially
            // the same as a simple delete.
            delete(id);
            return Table.column(Lists.newArrayList(id));
        } catch (HierarchyError e) {
            throw new RuntimeException(e);
        }
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
                    .page(manager.getEntityUnchecked(id, cls).getNarrowerConcepts(), Concept.class));
            tx.success();
            return page;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}")
    @Override
    public Response createChild(@PathParam("id") String id,
                                @QueryParam(ACCESSOR_PARAM) List<String> accessors, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = beginTx()) {
            final Concept parent = api().get(id, cls);
            Response item = createItem(bundle, accessors, concept -> {
                parent.addNarrowerConcept(concept);
                concept.setVocabulary(parent.getVocabulary());
            }, api().withScope(parent.getVocabulary()), cls);
            tx.success();
            return item;
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/parent")
    @Override
    public Response setParents(
            @PathParam("id") String id,
            @QueryParam(ID_PARAM) List<String> parentIds)
            throws PermissionDenied, ItemNotFound, DeserializationError {
        try (final Tx tx = beginTx()) {
            Response item = single(api().concepts().setBroaderConcepts(id, parentIds));
            tx.success();
            return item;
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/broader")
    public Response setBroaderCvocConcepts(
            @PathParam("id") String id,
            @QueryParam(ID_PARAM) List<String> broader
    ) throws PermissionDenied, ItemNotFound, DeserializationError {
        try (final Tx tx = beginTx()) {
            Response item = single(api().concepts().setBroaderConcepts(id, broader));
            tx.success();
            return item;
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/narrower")
    public Response addNarrowerCvocConcept(
            @PathParam("id") String id,
            @QueryParam(ID_PARAM) List<String> narrower)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = single(api().concepts()
                    .addNarrowerConcepts(id, narrower));
            tx.success();
            return item;
        }
    }

    @DELETE
    @Path("{id:[^/]+}/narrower")
    public Response removeNarrowerCvocConcept(
            @PathParam("id") String id,
            @QueryParam(ID_PARAM) List<String> narrower)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = single(api().concepts()
                    .removeNarrowerConcepts(id, narrower));
            tx.success();
            return item;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/broader")
    public Response getCvocBroaderConcepts(@PathParam("id") String id)
            throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, cls);
            Response list = streamingList(() -> manager.getEntityUnchecked(id, cls).getBroaderConcepts());
            tx.success();
            return list;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/related")
    public Response getCvocRelatedConcepts(@PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, cls);
            Response list = streamingList(() -> manager.getEntityUnchecked(id, cls).getRelatedConcepts());
            tx.success();
            return list;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id:[^/]+}/relatedBy")
    public Response getCvocRelatedByConcepts(@PathParam("id") String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            checkExists(id, cls);
            Response list = streamingList(() -> manager.getEntityUnchecked(id, cls).getRelatedByConcepts());
            tx.success();
            return list;
        }
    }

    @POST
    @Path("{id:[^/]+}/related")
    public Response addRelatedCvocConcept(
            @PathParam("id") String id,
            @QueryParam(ID_PARAM) List<String> related)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = single(api().concepts()
                    .addRelatedConcepts(id, related));
            tx.success();
            return item;
        }
    }

    @DELETE
    @Path("{id:[^/]+}/related")
    public Response removeRelatedCvocConcept(
            @PathParam("id") String id,
            @QueryParam(ID_PARAM) List<String> related)
            throws PermissionDenied, ItemNotFound {
        try (final Tx tx = beginTx()) {
            Response item = single(api().concepts()
                    .removeRelatedConcepts(id, related));
            tx.success();
            return item;
        }
    }
}
