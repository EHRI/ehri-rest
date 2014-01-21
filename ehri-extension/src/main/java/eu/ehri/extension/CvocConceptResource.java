package eu.ehri.extension;

import java.net.URI;
import java.util.List;

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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.views.ViewHelper;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.Query;

/**
 * Provides a RESTfull interface for the cvoc.Concept. Note that the concept
 * creation endpoint is part of the VocabularyResource and creation without a
 * Vocabulary is not possible via this API
 */
@Path(Entities.CVOC_CONCEPT)
public class CvocConceptResource extends
        AbstractAccessibleEntityResource<Concept> {

    private final ViewHelper helper;

    public CvocConceptResource(@Context GraphDatabaseService database) {
        super(database, Concept.class);
        helper = new ViewHelper(graph);
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
    public StreamingOutput listCvocConcepts(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return list(offset, limit, order, filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    public Response countCvocConcepts(@QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return count(filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/page")
    public StreamingOutput pageCvocConcepts(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return page(offset, limit, order, filters);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateCvocConcept(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
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
    public StreamingOutput getCvocNarrowerConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        Concept concept = views.detail(manager.getFrame(id, cls),
                getRequesterUserProfile());
        return streamingList(concept.getNarrowerConcepts());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    public StreamingOutput listCvocNarrowerConcepts(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)

    throws ItemNotFound, AccessDenied, BadRequester {
        Accessor user = getRequesterUserProfile();
        Concept concept = views.detail(manager.getFrame(id, cls), user);
        Query<Concept> query = new Query<Concept>(graph, Concept.class)
                .setLimit(limit).setOffset(offset).orderBy(order)
                .filter(filters);
        return streamingList(query.list(concept.getNarrowerConcepts(), user));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public Response countCvocNarrowerConcepts(
            @PathParam("id") String id,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        Concept concept = views.detail(manager.getFrame(id, cls), user);
        Query<Concept> query = new Query<Concept>(graph, Concept.class)
                .filter(filters);
        return Response.ok((query.count(concept.getNarrowerConcepts(), user))
                .toString().getBytes()).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/page")
    public StreamingOutput pageCvocNarrowerConcepts(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)

    throws ItemNotFound, AccessDenied, BadRequester {
        Accessor user = getRequesterUserProfile();
        Concept concept = views.detail(manager.getFrame(id, cls), user);
        Query<Concept> query = new Query<Concept>(graph, Concept.class)
                .setLimit(limit).setOffset(offset).orderBy(order)
                .filter(filters);
        return streamingPage(query.page(concept.getNarrowerConcepts(), user));
    }

    /**
     * Add an existing concept to the list of 'narrower' of this existing
     * Concepts No vertex is created, but the 'narrower' edge is created between
     * the two concept vertices.
     */
    @POST
    @Path("/{id:.+}/narrower/{id_narrower:.+}")
    public Response addNarrowerCvocConcept(
                @PathParam("id") String id,
                @PathParam("id_narrower") String id_narrower)
            throws AccessDenied, PermissionDenied, ItemNotFound, BadRequester {
        Accessor accessor = getRequesterUserProfile();
        Concept concept = views.detail(manager.getFrame(id, cls), accessor);
        Concept relatedConcept = views.detail(manager.getFrame(id_narrower, cls), accessor);
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
    @Path("/{id:.+}/narrower/{id_narrower:.+}")
    public Response removeNarrowerCvocConcept(
                @PathParam("id") String id,
                @PathParam("id_narrower") String id_narrower)
            throws PermissionDenied, AccessDenied, ItemNotFound, BadRequester {

        Accessor accessor = getRequesterUserProfile();
        Concept concept = views.detail(manager.getFrame(id, cls), accessor);
        Concept relatedConcept = views.detail(manager.getFrame(id_narrower, cls), accessor);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        helper.checkEntityPermission(relatedConcept, accessor, PermissionType.UPDATE);
        concept.removeNarrowerConcept(relatedConcept);
        graph.getBaseGraph().commit();
        return Response.status(Status.OK).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/broader/list")
    public StreamingOutput getCvocBroaderConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {

        Concept concept = views.detail(manager.getFrame(id, cls),
                getRequesterUserProfile());

        return streamingList(concept.getBroaderConcepts());
    }

    // See the relatedBy for the 'reverse' relation
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/related/list")
    public StreamingOutput getCvocRelatedConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {

        Concept concept = views.detail(manager.getFrame(id, cls),
                getRequesterUserProfile());

        return streamingList(concept.getRelatedConcepts());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/relatedBy/list")
    public StreamingOutput getCvocRelatedByConcepts(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {

        Concept concept = views.detail(manager.getFrame(id, cls),
                getRequesterUserProfile());

        return streamingList(concept.getRelatedByConcepts());
    }

    /**
     * Add a relation by creating the 'related' edge between the two concepts,
     * no vertex created
     */
    @POST
    @Path("/{id:.+}/related/{id_related:.+}")
    public Response addRelatedCvocConcept(
            @PathParam("id") String id,
            @PathParam("id_related") String id_related)
            throws AccessDenied, PermissionDenied, ItemNotFound, BadRequester {

        Accessor accessor = getRequesterUserProfile();
        Concept concept = views.detail(manager.getFrame(id, cls), accessor);
        Concept relatedConcept = views.detail(manager.getFrame(id_related, cls), accessor);
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
    @Path("/{id:.+}/related/{id_related:.+}")
    public Response removeRelatedCvocConcept(
                @PathParam("id") String id,
                @PathParam("id_related") String id_related)
            throws AccessDenied, PermissionDenied, ItemNotFound, BadRequester {

        Accessor accessor = getRequesterUserProfile();
        Concept concept = views.detail(manager.getFrame(id, cls), accessor);
        Concept relatedConcept = views.detail(manager.getFrame(id_related, cls), accessor);
        helper.checkEntityPermission(concept, accessor, PermissionType.UPDATE);
        helper.checkEntityPermission(relatedConcept, accessor, PermissionType.UPDATE);
        concept.removeRelatedConcept(relatedConcept);
        graph.getBaseGraph().commit();
        return Response.status(Status.OK).build();
    }

    /**
     * Create a top-level concept unit for this vocabulary.
     * 
     * @param id
     * @param json
     * @return
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
            String json, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        Concept parent = views.detail(manager.getFrame(id, cls), user);
        try {
            Concept concept = createConcept(json, parent);
            new AclManager(graph).setAccessors(concept, getAccessors(accessors, user));
            graph.getBaseGraph().commit();
            return buildResponseFromConcept(concept);
        } catch (SerializationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } finally {
            cleanupTransaction();
        }
    }

    // Helpers

    private Response buildResponseFromConcept(Concept concept)
            throws SerializationError {
        String jsonStr = getSerializer().vertexFrameToJson(concept);
        // FIXME: Hide the details of building this path
        URI docUri = UriBuilder.fromUri(uriInfo.getBaseUri())
                .segment(Entities.CVOC_CONCEPT)
                .segment(concept.getId())
                .build();

        return Response.status(Status.CREATED).location(docUri)
                .entity((jsonStr).getBytes()).build();
    }

    private Concept createConcept(String json, Concept parent)
            throws DeserializationError, PermissionDenied, ValidationError,
            IntegrityError, BadRequester {
        Bundle entityBundle = Bundle.fromString(json);

        // NB: Because concepts aren't strictly hierarchical (they can have
        // more than one broader term) we use the containing vocabulary as
        // the permission scope here, rather than the immediate parent.
        Concept concept = new LoggingCrudViews<Concept>(graph, Concept.class,
                parent.getPermissionScope()).create(entityBundle,
                getRequesterUserProfile(), getLogMessage());

        // Add it to this Vocabulary's concepts
        parent.addNarrowerConcept(concept);
        concept.setVocabulary(parent.getVocabulary());
        return concept;
    }
}
