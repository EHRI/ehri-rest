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

import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.ParentResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.cvoc.Concept;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

/**
 * Provides a web service interface for the Concept model. Note that the concept
 * creation endpoint is part of the VocabularyResource and creation without a
 * Vocabulary is not possible via this API.
 *
 * @author Paul Boon (http://github.com/PaulBoon)
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.CVOC_CONCEPT)
public class CvocConceptResource
        extends AbstractAccessibleEntityResource<Concept>
        implements ParentResource, GetResource, ListResource, UpdateResource, DeleteResource {

    public CvocConceptResource(@Context GraphDatabaseService database) {
        super(database, Concept.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    @Override
    public Response get(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return getItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    @Override
    public Response list() throws BadRequester {
        return listItems();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    @Override
    public long count() throws BadRequester {
        return countItems();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Override
    public Response update(Bundle bundle) throws PermissionDenied,
            ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return updateItem(bundle);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return updateItem(id, bundle);
    }

    @DELETE
    @Path("/{id:.+}")
    @Override
    public Response delete(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        return deleteItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    @Override
    public Response listChildren(@PathParam("id") String id,
                                 @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        Concept concept = views.detail(id, user);
        return streamingPage(getQuery(Concept.class)
                .page(concept.getNarrowerConcepts(), user));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    @Override
    public long countChildren(@PathParam("id") String id,
                              @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        Concept concept = views.detail(id, user);
        return getQuery(Concept.class).count(concept.getNarrowerConcepts());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.CVOC_CONCEPT)
    @Override
    public Response createChild(@PathParam("id") String id,
                                Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        final Accessor user = getRequesterUserProfile();
        final Concept parent = views.detail(id, user);
        return createItem(bundle, accessors, new Handler<Concept>() {
            @Override
            public void process(Concept concept) {
                parent.addNarrowerConcept(concept);
                concept.setVocabulary(parent.getVocabulary());
            }
        }, views.setScope(parent.getVocabulary()));
    }

    /**
     * Add an existing concept to the list of 'narrower' relations.
     *
     * @param id         The item ID
     * @param idNarrower The narrower item ID
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Path("/{id:.+}/narrower/{idNarrower:.+}")
    public Response addNarrowerCvocConcept(
            @PathParam("id") String id,
            @PathParam("idNarrower") String idNarrower)
            throws AccessDenied, PermissionDenied, ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        Concept concept = views.detail(id, accessor);
        Concept relatedConcept = views.detail(idNarrower, accessor);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        helper.checkEntityPermission(relatedConcept, accessor, PermissionType.UPDATE);
        concept.addNarrowerConcept(relatedConcept);
        graph.getBaseGraph().commit();
        return Response.status(Status.OK).build();
    }

    /**
     * Remove a narrower relationship between two concepts.
     *
     * @param id         The item ID
     * @param idNarrower The narrower item ID
     * @throws PermissionDenied,
     * @throws AccessDenied
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:.+}/narrower/{idNarrower:.+}")
    public Response removeNarrowerCvocConcept(
            @PathParam("id") String id,
            @PathParam("idNarrower") String idNarrower)
            throws PermissionDenied, AccessDenied, ItemNotFound, BadRequester {

        Accessor accessor = getRequesterUserProfile();
        Concept concept = views.detail(id, accessor);
        Concept relatedConcept = views.detail(idNarrower, accessor);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        helper.checkEntityPermission(relatedConcept, accessor, PermissionType.UPDATE);
        concept.removeNarrowerConcept(relatedConcept);
        graph.getBaseGraph().commit();
        return Response.status(Status.OK).build();
    }

    /**
     * List broader concepts.
     *
     * @param id The item ID
     * @return A list of broader resources
     * @throws ItemNotFound
     * @throws AccessDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/broader/list")
    public Response getCvocBroaderConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {

        Concept concept = views.detail(id, getRequesterUserProfile());

        return streamingList(concept.getBroaderConcepts());
    }

    /**
     * List concepts related to another concept.
     *
     * @param id The item ID
     * @return A list of related resources
     * @throws ItemNotFound
     * @throws AccessDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/related/list")
    public Response getCvocRelatedConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        Concept concept = views.detail(id, getRequesterUserProfile());
        return streamingList(concept.getRelatedConcepts());
    }

    /**
     * List concepts related to another concept. This is the
     * &quot;reverse&quot; form of the &quot;/related&quot;
     * method.
     *
     * @param id The item ID
     * @return A list of related resources
     * @throws ItemNotFound
     * @throws AccessDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/relatedBy/list")
    public Response getCvocRelatedByConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        Concept concept = views.detail(id, getRequesterUserProfile());
        return streamingList(concept.getRelatedByConcepts());
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
     * @throws BadRequester
     */
    @POST
    @Path("/{id:.+}/related/{idRelated:.+}")
    public Response addRelatedCvocConcept(
            @PathParam("id") String id,
            @PathParam("idRelated") String idRelated)
            throws AccessDenied, PermissionDenied, ItemNotFound, BadRequester {

        Accessor accessor = getRequesterUserProfile();
        Concept concept = views.detail(id, accessor);
        Concept relatedConcept = views.detail(idRelated, accessor);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        helper.checkEntityPermission(relatedConcept, accessor, PermissionType.UPDATE);
        concept.addRelatedConcept(relatedConcept);
        graph.getBaseGraph().commit();
        return Response.status(Status.OK).build();
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
     * @throws BadRequester
     */
    @DELETE
    @Path("/{id:.+}/related/{idRelated:.+}")
    public Response removeRelatedCvocConcept(
            @PathParam("id") String id,
            @PathParam("idRelated") String idRelated)
            throws AccessDenied, PermissionDenied, ItemNotFound, BadRequester {

        Accessor accessor = getRequesterUserProfile();
        Concept concept = views.detail(id, accessor);
        Concept relatedConcept = views.detail(idRelated, accessor);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        helper.checkEntityPermission(relatedConcept, accessor, PermissionType.UPDATE);
        concept.removeRelatedConcept(relatedConcept);
        graph.getBaseGraph().commit();
        return Response.status(Status.OK).build();
    }
}
