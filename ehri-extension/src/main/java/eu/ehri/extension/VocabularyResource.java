package eu.ehri.extension;

import java.net.URI;

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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.impl.Query;

/**
 * Provides a RESTfull interface for the Vocabulary
 * Also for managing the Concepts that are in the Vocabulary
 * 
 * @author paulboon
 *
 */
@Path(Entities.CVOC_VOCABULARY)
public class VocabularyResource extends AbstractAccessibleEntityResource<Vocabulary> {

    public VocabularyResource(@Context GraphDatabaseService database) {
        super(database, Vocabulary.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response getVocabulary(@PathParam("id") String id) throws ItemNotFound,
            PermissionDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listVocabularies(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit)
            throws ItemNotFound, BadRequester {
        return list(offset, limit);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}/list")
    public StreamingOutput listVocabularyConcepts(
            @PathParam("id") String id,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit)
            throws ItemNotFound, BadRequester, PermissionDenied {
        Vocabulary vocabulary = new Query<Vocabulary>(graph, Vocabulary.class).get(id,
                getRequesterUserProfile());
        return list(vocabulary.getConcepts(), offset, limit);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/page")
    public StreamingOutput pageVocabularies(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit)
            throws ItemNotFound, BadRequester {
        return page(offset, limit);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVocabulary(String json) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {
        return create(json);
    }

    // Note: json contains id
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVocabulary(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response updateVocabulary(@PathParam("id") String id, String json)
            throws PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteVocabulary(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }

    /*** Concept manipulation ***/
    
    // NOTE no id as long called!
    /**
     * Create an instance of the 'entity' in the database
     * 
     * @param json
     *            The json representation of the entity to create (no vertex
     *            'id' fields)
     * @return The response of the create request, the 'location' will contain
     *         the url of the newly created instance.
     * @throws PermissionDenied
     * @throws IntegrityError
     * @throws ValidationError
     * @throws DeserializationError
     * @throws SerializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response createVocabularyConcept(@PathParam("id") long id,
            String json) throws PermissionDenied, ValidationError,
            IntegrityError, DeserializationError, SerializationError,
            BadRequester {
    	Vocabulary vocabulary = views.detail(graph.getVertex(id, cls),
                getRequesterUserProfile());
    	Concept concept = createConcept(json, vocabulary);
        return buildResponseFromConcept(concept);
    }

    /**
     * Create a documentary unit for this repository.
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}/" + Entities.CVOC_CONCEPT)
    public Response createVocabularyConcept(@PathParam("id") String id,
            String json) throws PermissionDenied, ValidationError,
            IntegrityError, DeserializationError, ItemNotFound, BadRequester {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
        	Vocabulary vocabulary = new Query<Vocabulary>(graph, Vocabulary.class).get(id,
                    getRequesterUserProfile());
        	Concept concept = createConcept(json, vocabulary);
            tx.success();
            return buildResponseFromConcept(concept);
        } catch (SerializationError e) {
            tx.failure();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
    }

    // Helpers

    private Response buildResponseFromConcept(Concept concept)
            throws SerializationError {
        String jsonStr = converter.vertexFrameToJson(concept);
        // FIXME: Hide the details of building this path
        URI docUri = UriBuilder.fromUri(uriInfo.getBaseUri())
                .segment(Entities.CVOC_CONCEPT).segment(manager.getId(concept))
                .build();

        return Response.status(Status.CREATED).location(docUri)
                .entity((jsonStr).getBytes()).build();
    }

    private Concept createConcept(String json, Vocabulary vocabulary)
            throws DeserializationError, PermissionDenied, ValidationError,
            IntegrityError, BadRequester {
        Bundle entityBundle = converter.jsonToBundle(json);

        Concept concept = new LoggingCrudViews<Concept>(graph,
        		Concept.class, vocabulary)
                .create(entityBundle,
                        getRequesterUserProfile());
        
        // Add it to this Vocabulary's concepts
        concept.setVocabulary(vocabulary);
        concept.setPermissionScope(vocabulary);
        return concept;
    }
    
}
