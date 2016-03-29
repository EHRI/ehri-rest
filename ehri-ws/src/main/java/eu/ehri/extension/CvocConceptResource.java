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

import eu.ehri.extension.base.*;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.persistence.Bundle;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

/**
 * Provides a web service interface for the Concept model. Note that the concept
 * creation endpoint is part of the VocabularyResource and creation without a
 * Vocabulary is not possible via this API.
 */
@Path(AbstractRestResource.RESOURCE_ENDPOINT_PREFIX + "/" + Entities.CVOC_CONCEPT)
public class CvocConceptResource
        extends AbstractAccessibleResource<Concept>
        implements ParentResource, GetResource, ListResource, UpdateResource, DeleteResource {

    public CvocConceptResource(@Context GraphDatabaseService database) {
        super(database, Concept.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:[^/]+}")
    @Override
    public Response get(@PathParam("id") String id) throws ItemNotFound {
        return getItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Override
    public Response list() {
        return listItems();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:[^/]+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Response item = updateItem(id, bundle);
            tx.success();
            return item;
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

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:[^/]+}/list")
    @Override
    public Response listChildren(@PathParam("id") String id,
                                 @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            Concept concept = views.detail(id, user);
            return streamingPage(getQuery(Concept.class)
                    .page(concept.getNarrowerConcepts(), user), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:[^/]+}/" + Entities.CVOC_CONCEPT)
    @Override
    public Response createChild(@PathParam("id") String id,
                                Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor user = getRequesterUserProfile();
            final Concept parent = views.detail(id, user);
            Response item = createItem(bundle, accessors, new Handler<Concept>() {
                @Override
                public void process(Concept concept) {
                    parent.addNarrowerConcept(concept);
                    concept.setVocabulary(parent.getVocabulary());
                }
            }, views.setScope(parent.getVocabulary()));
            tx.success();
            return item;
        }
    }

    /**
     * Add an existing concept to the list of 'narrower' relations.
     *
     * @param id         The item ID
     * @param idNarrower The narrower item ID
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @POST
    @Path("{id:[^/]+}/narrower/{idNarrower:[^/]+}")
    public Response addNarrowerCvocConcept(
            @PathParam("id") String id,
            @PathParam("idNarrower") String idNarrower)
            throws AccessDenied, PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            Concept concept = views.detail(id, accessor);
            Concept relatedConcept = views.detail(idNarrower, accessor);
            helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
            helper.checkEntityPermission(relatedConcept, accessor, PermissionType.UPDATE);
            concept.addNarrowerConcept(relatedConcept);
            tx.success();
            return Response.status(Status.OK).build();
        }
    }

    /**
     * Remove a narrower relationship between two concepts.
     *
     * @param id         The item ID
     * @param idNarrower The narrower item ID
     * @throws AccessDenied
     * @throws ItemNotFound
     */
    @DELETE
    @Path("{id:[^/]+}/narrower/{idNarrower:[^/]+}")
    public Response removeNarrowerCvocConcept(
            @PathParam("id") String id,
            @PathParam("idNarrower") String idNarrower)
            throws PermissionDenied, AccessDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            Concept concept = views.detail(id, accessor);
            Concept relatedConcept = views.detail(idNarrower, accessor);
            helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
            helper.checkEntityPermission(relatedConcept, accessor, PermissionType.UPDATE);
            concept.removeNarrowerConcept(relatedConcept);
            tx.success();
            return Response.status(Status.OK).build();
        }
    }

    /**
     * List broader concepts.
     *
     * @param id The item ID
     * @return A list of broader resources
     * @throws ItemNotFound
     * @throws AccessDenied
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:[^/]+}/broader/list")
    public Response getCvocBroaderConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Concept concept = views.detail(id, getRequesterUserProfile());
            return streamingList(concept.getBroaderConcepts(), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * List concepts related to another concept.
     *
     * @param id The item ID
     * @return A list of related resources
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:[^/]+}/related/list")
    public Response getCvocRelatedConcepts(@PathParam("id") String id) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Concept concept = views.detail(id, getRequesterUserProfile());
            return streamingList(concept.getRelatedConcepts(), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * List concepts related to another concept. This is the
     * &quot;reverse&quot; form of the &quot;/related&quot;
     * method.
     *
     * @param id The item ID
     * @return A list of related resources
     * @throws ItemNotFound
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("{id:[^/]+}/relatedBy/list")
    public Response getCvocRelatedByConcepts(@PathParam("id") String id) throws ItemNotFound {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            Concept concept = views.detail(id, getRequesterUserProfile());
            return streamingList(concept.getRelatedByConcepts(), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Add a relation by creating the 'related' edge between the two <em>existing</em>
     * items.
     *
     * @param id        The item ID
     * @param idRelated The related item ID
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @POST
    @Path("{id:[^/]+}/related/{idRelated:[^/]+}")
    public Response addRelatedCvocConcept(
            @PathParam("id") String id,
            @PathParam("idRelated") String idRelated)
            throws AccessDenied, PermissionDenied, ItemNotFound {

        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            Concept concept = views.detail(id, accessor);
            Concept relatedConcept = views.detail(idRelated, accessor);
            helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
            helper.checkEntityPermission(relatedConcept, accessor, PermissionType.UPDATE);
            concept.addRelatedConcept(relatedConcept);
            tx.success();
            return Response.status(Status.OK).build();
        }
    }

    /**
     * Remove a relation by deleting the edge, not the vertex of the related
     * concept.
     *
     * @param id        The item ID
     * @param idRelated The related item ID
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ItemNotFound
     */
    @DELETE
    @Path("{id:[^/]+}/related/{idRelated:[^/]+}")
    public Response removeRelatedCvocConcept(
            @PathParam("id") String id,
            @PathParam("idRelated") String idRelated)
            throws AccessDenied, PermissionDenied, ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            Accessor accessor = getRequesterUserProfile();
            Concept concept = views.detail(id, accessor);
            Concept relatedConcept = views.detail(idRelated, accessor);
            helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
            helper.checkEntityPermission(relatedConcept, accessor, PermissionType.UPDATE);
            concept.removeRelatedConcept(relatedConcept);
            tx.success();
            return Response.status(Status.OK).build();
        }
    }
}
