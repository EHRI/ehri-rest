package eu.ehri.extension;

import java.io.IOException;
import java.io.OutputStream;

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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
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

/**
 * Provides a RESTfull interface for the cvoc.Concept.
 */
@Path(Entities.CVOC_CONCEPT)
public class CvocConceptResource extends EhriNeo4jFramedResource<Concept> {

    public CvocConceptResource(@Context GraphDatabaseService database) {
        super(database, Concept.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCvocConcept(@QueryParam("key") String key,
            @QueryParam("value") String value) throws ItemNotFound,
            PermissionDenied, BadRequester {
        return retrieve(key, value);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response getCvocConcept(@PathParam("id") long id)
            throws PermissionDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response getCvocConcept(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listCvocConcepts(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit)
            throws ItemNotFound, BadRequester {
        return list(offset, limit);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCvocConcept(String json) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {
        return create(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCvocConcept(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response updateCvocConcept(@PathParam("id") String id, String json)
            throws PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id:\\d+}")
    public Response deleteCvocConcept(@PathParam("id") long id)
            throws PermissionDenied, ValidationError, ItemNotFound,
            BadRequester {
        return delete(id);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteCvocConcept(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }
    
    /*** 'related' concepts ***/
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}/narrower/list")
    public StreamingOutput getCvocNarrowerConcepts(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied, BadRequester {

    	Concept concept = views.detail(manager.getFrame(id, cls), getRequesterUserProfile());
 
    	return getListAsJson(concept.getNarrowerConcepts());
    }

    /**
     * Add an existing concept to the list of 'narrower' of this existing Concepts
	 * No vertex is created, but the 'narrower' edge is created between the two concept vertices. 
	 * 
	 * Note: the internal vertex id's are used now, but we want identifiers
     */
    @POST
    @Path("/{id:.+}/narrower/{id_narrower:.+}")
    public Response addNarrowerCvocConcept(String json, 
    		@PathParam("id") String id,
    		@PathParam("id_narrower") String id_narrower) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {
    	    	
		Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
		try {
			Concept concept = views.detail(manager.getFrame(id, cls), getRequesterUserProfile());
			Concept narrowerConcept = views.detail(manager.getFrame(id_narrower, cls), getRequesterUserProfile());
			concept.addNarrowerConcept(narrowerConcept);
		    tx.success();
		    return Response.status(Status.OK).build();
		} catch (Exception e) {
		    tx.failure();
		    throw new WebApplicationException(e);
		} finally {
		    tx.finish();
		}   	
    }

    /**
     *  Removing the narrower relation 
     *  by deleting the edge, not the vertex of the narrower concept 
     */
    @DELETE
    @Path("/{id:.+}/narrower/{id_narrower:.+}")
    public Response removeNarrowerCvocConcept(String json, 
    		@PathParam("id") String id,
    		@PathParam("id_narrower") String id_narrower) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {
    	    	
		Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
		try {
			Concept concept = views.detail(manager.getFrame(id, cls), getRequesterUserProfile());
			Concept narrowerConcept = views.detail(manager.getFrame(id_narrower, cls), getRequesterUserProfile());
			concept.removeNarrowerConcept(narrowerConcept);

		    tx.success();
		    return Response.status(Status.OK).build();
		} catch (Exception e) {
		    tx.failure();
		    throw new WebApplicationException(e);
		} finally {
		    tx.finish();
		}   	
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}/broader/list")
    public StreamingOutput getCvocBroaderConcepts(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied, BadRequester {
    	
    	Concept concept = views.detail(manager.getFrame(id, cls), getRequesterUserProfile());

    	return getListAsJson(concept.getBroaderConcepts());
    }

    // See the relatedBy for the 'reverse' relation
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}/related/list")
    public StreamingOutput getCvocRelatedConcepts(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied, BadRequester {
    	
    	Concept concept = views.detail(manager.getFrame(id, cls), getRequesterUserProfile());

    	return getListAsJson(concept.getRelatedConcepts());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}/relatedBy/list")
    public StreamingOutput getCvocRelatedByConcepts(@PathParam("id") String id)
            throws ItemNotFound, PermissionDenied, BadRequester {
    	
    	Concept concept = views.detail(manager.getFrame(id, cls), getRequesterUserProfile());

    	return getListAsJson(concept.getRelatedByConcepts());
    }
    
    /**
     * Add a relation by creating the 'related' edge between the two concepts, no vertex created 
     */
    @POST
    @Path("/{id:.+}/related/{id_related:.+}")
    public Response addRelatedCvocConcept(String json, 
    		@PathParam("id") String id,
    		@PathParam("id_related") String id_related) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {
    	    	
		Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
		try {
			Concept concept = views.detail(manager.getFrame(id, cls), getRequesterUserProfile());
			Concept relatedConcept = views.detail(manager.getFrame(id_related, cls), getRequesterUserProfile());
			concept.addRelatedConcept(relatedConcept);
		    tx.success();
		    return Response.status(Status.OK).build();
		} catch (Exception e) {
		    tx.failure();
		    throw new WebApplicationException(e);
		} finally {
		    tx.finish();
		}
    }

    /**
     *  Remove a relation by deleting the edge, not the vertex of the related concept 
     */
    @DELETE
    @Path("/{id:.+}/related/{id_related:.+}")
    public Response removeRelatedCvocConcept(String json, 
    		@PathParam("id") String id,
    		@PathParam("id_related") String id_related) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {
    	    	
		Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
		try {
			Concept concept = views.detail(manager.getFrame(id, cls), getRequesterUserProfile());
			Concept relatedConcept = views.detail(manager.getFrame(id_related, cls), getRequesterUserProfile());
			concept.removeRelatedConcept(relatedConcept);

		    tx.success();
		    return Response.status(Status.OK).build();
		} catch (Exception e) {
		    tx.failure();
		    throw new WebApplicationException(e);
		} finally {
		    tx.finish();
		}   	
    }
    
    // TODO helper function 
	// NOTE: maybe we can make this generic and reuse it from EhriNeo4jFramedResource 
    public StreamingOutput getListAsJson(final Iterable<Concept> list)
            throws ItemNotFound, BadRequester {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonFactory f = new JsonFactory();

        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException,
                    WebApplicationException {
                JsonGenerator g = f.createJsonGenerator(arg0);
                g.writeStartArray();
                for (Concept item : list) {
                    try {
                        mapper.writeValue(g, converter.vertexFrameToData(item));
                    } catch (SerializationError e) {
                        throw new RuntimeException(e);
                    }
                }
                g.writeEndArray();
                g.close();
            }
        };
    }
    
}
