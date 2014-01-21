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
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import eu.ehri.project.exceptions.*;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.Query;

/**
 * Provides a RESTfull interface for the Vocabulary Also for managing the
 * Concepts that are in the Vocabulary
 * 
 * @author paulboon
 * 
 */
@Path(Entities.CVOC_VOCABULARY)
public class VocabularyResource extends
        AbstractAccessibleEntityResource<Vocabulary> {

    public VocabularyResource(@Context GraphDatabaseService database) {
        super(database, Vocabulary.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getVocabulary(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public StreamingOutput listVocabularies(
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
    public Response countVocabularies(@QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return count(filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/page")
    public StreamingOutput pageVocabularies(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return page(offset, limit, order, filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    public StreamingOutput listVocabularyConcepts(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        Vocabulary vocabulary = views.detail(manager.getFrame(id, cls), user);
        Query<Concept> query = new Query<Concept>(graph, Concept.class)
                .setLimit(limit).setOffset(offset).orderBy(order)
                .filter(filters);
        return streamingList(query.list(vocabulary.getConcepts(), user));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public Response countVocabularyConcepts(
            @PathParam("id") String id,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        Vocabulary vocabulary = views.detail(manager.getFrame(id, cls), user);
        Query<Concept> query = new Query<Concept>(graph, Concept.class)
                .filter(filters);
        return Response.ok((query.count(vocabulary.getConcepts(), user))
                .toString().getBytes()).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/page")
    public StreamingOutput pageVocabularyConcepts(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        Vocabulary vocabulary = views.detail(manager.getFrame(id, cls), user);
        Query<Concept> query = new Query<Concept>(graph, Concept.class)
                .setLimit(limit).setOffset(offset).orderBy(order)
                .filter(filters);
        return streamingPage(query.page(vocabulary.getConcepts(), user));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createVocabulary(String json,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        return create(json, accessors);
    }

    // Note: json contains id
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateVocabulary(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateVocabulary(@PathParam("id") String id, String json)
            throws AccessDenied, PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteVocabulary(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }

    /*** Concept manipulation ***/

    @DELETE
    @Path("/{id:.+}/all")
    public Response deleteAllVocabularyConcepts(
            @PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied, PermissionDenied {
        Vocabulary vocabulary = new Query<Vocabulary>(graph, Vocabulary.class).get(id,
                getRequesterUserProfile());
        
        //return deleteAllConcepts(vocabulary);
        try {
        	LoggingCrudViews<Concept> conceptViews = new LoggingCrudViews<Concept>(graph,
            		Concept.class, vocabulary);
        	Accessor requesterUserProfile = getRequesterUserProfile();
        	Iterable<Concept> concepts = vocabulary.getConcepts();
        	for (Concept concept : concepts) {
        		conceptViews.delete(concept, requesterUserProfile);
        	}
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        } catch (SerializationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } catch (ValidationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Create a top-level concept unit for this vocabulary.
     * 
     * @param id
     * @param json
     * @return
     * @throws PermissionDenied
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
    public Response createVocabularyConcept(@PathParam("id") String id,
            String json, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
                DeserializationError, ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        Vocabulary vocabulary = views.detail(manager.getFrame(id, cls), user);
        try {
            Concept concept = createConcept(json, vocabulary);
            new AclManager(graph).setAccessors(concept,
                    getAccessors(accessors, user));
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

    private Concept createConcept(String json, Vocabulary vocabulary)
            throws DeserializationError, PermissionDenied, ValidationError,
            IntegrityError, BadRequester {
        Bundle entityBundle = Bundle.fromString(json);

        Concept concept = new LoggingCrudViews<Concept>(graph, Concept.class,
                vocabulary).create(entityBundle, getRequesterUserProfile(), getLogMessage());

        // Add it to this Vocabulary's concepts
        concept.setVocabulary(vocabulary);
        return concept;
    }

}
