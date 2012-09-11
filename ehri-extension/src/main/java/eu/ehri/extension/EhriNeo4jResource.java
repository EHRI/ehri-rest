/**
 * TODO Add license text
 */
package eu.ehri.extension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.views.Views;

@Path( "/ehri" )
public class EhriNeo4jResource
{
    private final GraphDatabaseService database;

    public EhriNeo4jResource( @Context GraphDatabaseService database )
    {
        this.database = database;
    }

    /** 
     * The hello world example code ... should work
     * http://localhost:7474/examples/unmanaged/ehri/2
     * 
     * @param nodeId
     * @return
     */
    @GET
    @Produces( MediaType.TEXT_PLAIN )
    @Path( "/{nodeId}" )
    public Response hello( @PathParam( "nodeId" ) long nodeId )
    {
        // Do stuff with the database
        return Response.status( Status.OK ).entity(
                ( "Hello World, nodeId=" + nodeId ).getBytes() ).build();
    }
    
    
    // Try to get some data 
    // using the libs we made
    /**
     * http://localhost:7474/examples/unmanaged/ehri/documentaryUnit/2
     * 
     * @param indexName
     * @param nodeId
     * @return
     */
    @GET
    @Produces( MediaType.TEXT_PLAIN )
    @Path( "/documentaryUnit/{id}" )
    public Response getDocumentaryUnit( @PathParam( "id" ) long id )
    {
    	Long userId = 80497L; // HARDCODE just for testing, This is the neo4j node ID !!!
    	
	    FramedGraph<Neo4jGraph> graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(database));
	    Views<DocumentaryUnit> views = new Views<DocumentaryUnit>(graph, DocumentaryUnit.class);
	    try {
			DocumentaryUnit unit = views.detail(id, userId);
			String jsonStr = new Converter().vertexFrameToJson(unit);

			return Response.status( Status.OK ).entity(
	                ( jsonStr ).getBytes() ).build();
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED).build();
		} catch (SerializationError e) {
			// Most likely there was no such item (wrong id) 
			// so we would need to return a BADREQUEST
			return Response.status(Status.BAD_REQUEST).entity(("wrong id: " + id).getBytes()).build();
			
			// for testing 
			//return Response.status(Status.INTERNAL_SERVER_ERROR).entity(getStackTrace(e).getBytes()).build();
		}
     }
    
    /**
     * http://localhost:7474/examples/unmanaged/ehri/userProfile/80497
     * 
     * @param id
     * @return
     */
    @GET
    @Produces( MediaType.TEXT_PLAIN )
    @Path( "/userProfile/{id}" )
    public Response getUserProfile( @PathParam( "id" ) long id )
    {
    	Long userId = 80497L; // HARDCODE just for testing, This is the neo4j node ID !!!
    	
	    FramedGraph<Neo4jGraph> graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(database));
	    Views<UserProfile> views = new Views<UserProfile>(graph, UserProfile.class);
	    try {
	    	UserProfile unit = views.detail(id, userId);
			String jsonStr = new Converter().vertexFrameToJson(unit);

			return Response.status( Status.OK ).entity(
	                ( jsonStr ).getBytes() ).build();
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED).build();
		} catch (SerializationError e) {
			// Most likely there was no such item (wrong id) 
			// so we would need to return a BADREQUEST
			//return Response.status(Status.BAD_REQUEST).entity(("wrong id: " + id).getBytes()).build();
			
			// for testing 
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(getStackTrace(e).getBytes()).build();
		}
     }    
    
    
    
    // Use for testing
    // see http://www.javapractices.com/topic/TopicAction.do?Id=78
    // for even nicer trace tool
    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
      }
}