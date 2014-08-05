package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.PermissionType;
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
 * Provides a RESTful interface for the cvoc.Concept. Note that the concept
 * creation endpoint is part of the VocabularyResource and creation without a
 * Vocabulary is not possible via this API
 */
@Path(Entities.CVOC_CONCEPT)
public class CvocConceptResource extends
        AbstractAccessibleEntityResource<Concept> {

    public CvocConceptResource(@Context GraphDatabaseService database) {
        super(database, Concept.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getCvocConcept(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public Response listCvocConcepts() throws ItemNotFound, BadRequester {
        return page();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    public Response countCvocConcepts() throws ItemNotFound, BadRequester {
        return count();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateCvocConcept(Bundle bundle) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(bundle);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateCvocConcept(@PathParam("id") String id, String json)
            throws AccessDenied, PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteCvocConcept(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }

    /*** 'related' concepts ***/

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/narrower/list")
    public Response getCvocNarrowerConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        Concept concept = views.detail(id, getRequesterUserProfile());
        return streamingList(concept.getNarrowerConcepts());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    public Response listCvocNarrowerConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        Accessor user = getRequesterUserProfile();
        Concept concept = views.detail(id, user);
        return streamingPage(getQuery(Concept.class)
                .page(concept.getNarrowerConcepts(), user));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public Response countCvocNarrowerConcepts(@PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        Concept concept = views.detail(id, user);
        return numberResponse(getQuery(Concept.class)
                .count(concept.getNarrowerConcepts()));
    }

    /**
     * Add an existing concept to the list of 'narrower' of this existing
     * Concepts No vertex is created, but the 'narrower' edge is created between
     * the two concept vertices.
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
     * Removing the narrower relation by deleting the edge, not the vertex of
     * the narrower concept
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

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/broader/list")
    public Response getCvocBroaderConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {

        Concept concept = views.detail(id, getRequesterUserProfile());

        return streamingList(concept.getBroaderConcepts());
    }

    // See the relatedBy for the 'reverse' relation
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/related/list")
    public Response getCvocRelatedConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {

        Concept concept = views.detail(id, getRequesterUserProfile());

        return streamingList(concept.getRelatedConcepts());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/relatedBy/list")
    public Response getCvocRelatedByConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {

        Concept concept = views.detail(id, getRequesterUserProfile());

        return streamingList(concept.getRelatedByConcepts());
    }

    /**
     * Add a relation by creating the 'related' edge between the two concepts,
     * no vertex created
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
     * concept
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

    /**
     * Create a top-level concept unit for this vocabulary.
     * 
     * @param id The vocabulary id
     * @param bundle The new concept data
     * @return The new concept
     * @throws PermissionDenied
     * @throws AccessDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.CVOC_CONCEPT)
    public Response createNarrowerConcept(@PathParam("id") String id,
            Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        final Accessor user = getRequesterUserProfile();
        final Concept parent = views.detail(id, user);
        return create(bundle, accessors, new Handler<Concept>() {
            @Override
            public void process(Concept concept) {
                parent.addNarrowerConcept(concept);
                concept.setVocabulary(parent.getVocabulary());
            }
        });
    }
}
