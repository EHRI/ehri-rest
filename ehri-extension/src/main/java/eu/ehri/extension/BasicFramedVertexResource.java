package eu.ehri.extension;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.BasicFramedVertex;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistance.EntityBundle;
import eu.ehri.project.relationships.Annotates;
import eu.ehri.project.views.Views;

/**
 * Just to see if it makes sense 
 *
 */
@Path(EntityTypes.BASIC)
public class BasicFramedVertexResource extends
		EhriNeo4jFramedResource<BasicFramedVertex> {

	public BasicFramedVertexResource(@Context GraphDatabaseService database) {
		super(database, BasicFramedVertex.class);
	}
	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response getBasicFramedVertex(@PathParam("id") long id) {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:[\\w-]+}")
    public Response getBasicFramedVertex(@PathParam("id") String id) {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listBasicFramedVertexs() {
        return list();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("")
    public Response createBasicFramedVertex(String json) {
        return create(json);
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("")
    public Response updateBasicFramedVertex(String json) {
        return update(json);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteBasicFramedVertex(@PathParam("id") long id) {
        return delete(id);
    }

    // TODO put an annotation creator here
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/annotation")
    public Response createAnnotation(@PathParam("id") long id, String json) {
    	
    	// do make an annotation
    	
    	Long requesterUserProfileId = 0L;
    	try {
			requesterUserProfileId = getRequesterUserProfileId();
		} catch (PermissionDenied e1) {			
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return Response.status(Status.UNAUTHORIZED).entity("Wrong annotator").build();
		}
    	
    	Views<Annotation> annViews = new Views<Annotation>(graph, Annotation.class);
        EntityBundle<Annotation> entityBundle = null;
		try {
			entityBundle = converter.jsonToBundle(json);
		} catch (DeserializationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity("Wrong data").build();
		}
		Annotation annotation = null;
		try {
			annotation = annViews.create(converter.bundleToData(entityBundle), requesterUserProfileId);
		} catch (ValidationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity("Wrong data").build();
		} catch (DeserializationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity("Wrong data").build();
		} catch (PermissionDenied e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.status(Status.UNAUTHORIZED).entity("Wrong annotator").build();
		}

		// NOTE we don't have a single transaction with rollback now !
    	
		// create the relationships
		
		// to the target, could that have been done with the create?
		// Eeeerrrrrr, just create edges....
		Vertex targetVertex = graph.getBaseGraph().getVertex(id);
		Vertex userVertex = graph.getBaseGraph().getVertex(requesterUserProfileId);
		if (targetVertex == null || userVertex == null) {
			return Response.status(Status.BAD_REQUEST).entity("Wrong target and or annotator").build();
		}
		
		Map<String, Object> data = new HashMap<String, Object>(); // empty
		GraphHelpers helpers = new GraphHelpers(graph.getBaseGraph().getRawGraph());
		Index<Edge> annotatesIndex = helpers.getOrCreateIndex(Annotation.ANNOTATES, Edge.class);
        helpers.createIndexedEdge(annotation.asVertex(), targetVertex, Annotation.ANNOTATES, data, annotatesIndex);
		
        // from userProfile to annotation 'hasAnnotation'
		Index<Edge> hasAnnotationIndex = helpers.getOrCreateIndex(UserProfile.HAS_ANNOTATION, Edge.class);
        helpers.createIndexedEdge(userVertex, annotation.asVertex(), UserProfile.HAS_ANNOTATION, data, hasAnnotationIndex);
        
        
    	//return Response.status(200).entity("FAKE annotation").build();
        String jsonStr = null;
		try {
			jsonStr = converter.vertexFrameToJson(annotation);
		} catch (SerializationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI docUri = ub.path(annotation.asVertex().getId().toString()).build();

        return Response.status(Status.OK).location(docUri)
                .entity((jsonStr).getBytes()).build();
    }

}
