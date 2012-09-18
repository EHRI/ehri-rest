/**
 * TODO Add license text
 */
package eu.ehri.extension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.extension.test.EhriNeo4jExtensionRestClientTest;
import eu.ehri.project.core.GraphHelpers;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.persistance.EntityBundle;
import eu.ehri.project.views.Views;

/**
 * The EHRI internal API (RESTfull) 
 * for the collection (docuemntary units) registration system. 
 * Implemented as a neo4j unmanaged extension. 
 * Makes use of the ehri-data-frames java project to persist data in a graphdatabase (neo4j). 
 * More direct Vertex and Edge manipulation is provided by the ehri-plugin java project
 */
@Path("/ehri")
public class EhriNeo4jResource {
	//	private static final Logger logger = LoggerFactory.getLogger(EhriNeo4jResource.class);
	// Logging in the neo4j log instead of org.slf4j.Logger
	org.neo4j.server.logging.Logger logger = org.neo4j.server.logging.Logger.getLogger(EhriNeo4jResource.class);
	
	/**
	 * With each request the headers of that request are injected into the
	 * requestHeaders parameter.
	 */
	@Context
	private HttpHeaders requestHeaders;

	/**
	 * With each request URI info is injected into the uriInfo parameter.
	 */
	@Context
	private UriInfo uriInfo;

	private final GraphDatabaseService database;
	private FramedGraph<Neo4jGraph> graph;
	private Converter converter = new Converter();

	public final static String AUTH_HEADER_NAME = "Authorization";
	
	public EhriNeo4jResource(@Context GraphDatabaseService database) {
		this.database = database;
		graph = new FramedGraph<Neo4jGraph>(new Neo4jGraph(database));
	}

	/*** DocumentaryUnit ***/

	/**
	 * Get the DocumentaryUnit
	 * 
	 * @param id The vertex id of the DocumentaryUnit
	 * @return The response
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/documentaryUnit/{id}")
	public Response getDocumentaryUnit(@PathParam("id") long id) {
		Views<DocumentaryUnit> views = new Views<DocumentaryUnit>(graph,
				DocumentaryUnit.class);
		try {
			DocumentaryUnit entity = views
					.detail(id, getRequesterUserProfileId());
			String jsonStr = new Converter().vertexFrameToJson(entity);

			return Response.status(Status.OK).entity((jsonStr).getBytes())
					.build();
		} catch (PermissionDenied e) {
			logger.warn("Unauthorized access attempted", e);
			return Response.status(Status.UNAUTHORIZED).build();
		} catch (SerializationError e) {
			// Most likely there was no such item (wrong id)
			return Response.status(Status.BAD_REQUEST)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (Exception e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}
	}

	/**
	 * Create a DocumentaryUnit
	 * 
	 * @param json The data used to create
	 * @return The response
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/documentaryUnit")
	public Response createDocumentaryUnit(String json) {
		Views<DocumentaryUnit> views = new Views<DocumentaryUnit>(graph,
				DocumentaryUnit.class);

		EntityBundle<VertexFrame> entityBundle = null;
		try {
			entityBundle = converter.jsonToBundle(json);
		} catch (DeserializationError e1) {
			return Response.status(Status.BAD_REQUEST)
					.entity(produceErrorMessageJson(e1).getBytes()).build();
		}

		DocumentaryUnit enity = null;
		try {
			enity = views.create(converter.bundleToData(entityBundle),
					getRequesterUserProfileId());
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (ValidationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (DeserializationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}

		// Return the json of the created unit,
		// but what if it fails, the unit has already been created; no rollback!
		String jsonStr;
		try {
			jsonStr = converter.vertexFrameToJson(enity);
		} catch (SerializationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}

		// The caller wants to know the id of the created vertex
		// It is in the returned json but it is better if
		// the loacation holds the url to the new resource so that can be used
		// with a GET,
		// otherwise we would have to add a 'uri' or 'self' field to the json?
		UriBuilder ub = uriInfo.getAbsolutePathBuilder();
		URI docUri = ub.path(enity.asVertex().getId().toString()).build();

		return Response.status(Status.OK).location(docUri)
				.entity((jsonStr).getBytes()).build();
	}

	/**
	 * Update the DocumentaryUnit
	 * 
	 * @param json of the new data
	 * @return The response
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/documentaryUnit")
	public Response updateDocumentaryUnit(String json) {
		Views<DocumentaryUnit> views = new Views<DocumentaryUnit>(graph,
				DocumentaryUnit.class);

		EntityBundle<VertexFrame> entityBundle = null;
		try {
			entityBundle = converter.jsonToBundle(json);
		} catch (DeserializationError e1) {
			return Response.status(Status.BAD_REQUEST)
					.entity(produceErrorMessageJson(e1).getBytes()).build();
		}

		DocumentaryUnit unit = null;
		try {
			unit = views.update(converter.bundleToData(entityBundle),
					getRequesterUserProfileId());
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (ValidationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (DeserializationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}

		return Response.status(Status.OK).build();
	}

	/**
	 * Delete a DocumentaryUnit 
	 * (and what else?)
	 * 
	 * @param id The vertex id of the DocumentaryUnit to delete
	 * @return The response
	 */
	@DELETE
	@Path("/documentaryUnit/{id}")
	public Response deleteDocumentaryUnit(@PathParam("id") long id) {
		Views<DocumentaryUnit> views = new Views<DocumentaryUnit>(graph,
				DocumentaryUnit.class);

		try {
			views.delete(id, getRequesterUserProfileId());
			return Response.status(Status.OK).build();
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (ValidationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (SerializationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}

	}

	/*** UserProfile ***/

	/*
	 * NOTE maybe in another resource class (UserProfileResource), but I am not sure the neo4j
	 * extension mechanism allows that?
	 * 
	 * Also we could make an Abstract Base class that has template functions for the CRUD
	 */

	/**
	 * Get the UserProfile 
	 * 
	 * @param id The vertex id of the UserProfile
	 * @return The response
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/userProfile/{id}")
	public Response getUserProfile(@PathParam("id") long id) {
		Views<UserProfile> views = new Views<UserProfile>(graph,
				UserProfile.class);
		try {
			UserProfile unit = views.detail(id, getRequesterUserProfileId());
			String jsonStr = new Converter().vertexFrameToJson(unit);

			return Response.status(Status.OK).entity((jsonStr).getBytes())
					.build();
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED).build();
		} catch (SerializationError e) {
			// Most likely there was no such item (wrong id)
			// BETTER get a different Exception for that?
			//
			// so we would need to return a BADREQUEST, or NOTFOUND

			return Response.status(Status.NOT_FOUND)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}
	}

	/**
	 * Create a UserProfile
	 * 
	 * @param json The data used to create
	 * @return The response
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/userProfile")
	public Response createUserProfile(String json) {
		Views<UserProfile> views = new Views<UserProfile>(graph,
				UserProfile.class);

		EntityBundle<VertexFrame> entityBundle = null;
		try {
			entityBundle = converter.jsonToBundle(json);
		} catch (DeserializationError e1) {
			return Response.status(Status.BAD_REQUEST)
					.entity(produceErrorMessageJson(e1).getBytes()).build();
		}

		UserProfile entity = null;
		try {
			entity = views.create(converter.bundleToData(entityBundle),
					getRequesterUserProfileId());
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (ValidationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (DeserializationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}

		// Return the json of the created entity,
		// but what if it fails, the entity has already been created; no rollback!
		String jsonStr;
		try {
			jsonStr = converter.vertexFrameToJson(entity);
		} catch (SerializationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}

		// The caller wants to know the id of the created vertex
		// It is in the returned json but it is better if
		// the loacation holds the url to the new resource so that can be used
		// with a GET,
		// otherwise we would have to add a 'uri' or 'self' field to the json?
		UriBuilder ub = uriInfo.getAbsolutePathBuilder();
		URI docUri = ub.path(entity.asVertex().getId().toString()).build();

		return Response.status(Status.OK).location(docUri)
				.entity((jsonStr).getBytes()).build();
	}
	
	/**
	 * Update the DocumentaryUnit
	 * 
	 * @param json of the new data
	 * @return The response
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/userProfile")
	public Response updateUserProfile(String json) {
		Views<UserProfile> views = new Views<UserProfile>(graph,
				UserProfile.class);

		EntityBundle<VertexFrame> entityBundle = null;
		try {
			entityBundle = converter.jsonToBundle(json);
		} catch (DeserializationError e1) {
			return Response.status(Status.BAD_REQUEST)
					.entity(produceErrorMessageJson(e1).getBytes()).build();
		}

		UserProfile entity = null;
		try {
			entity = views.update(converter.bundleToData(entityBundle),
					getRequesterUserProfileId());
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (ValidationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (DeserializationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}

		return Response.status(Status.OK).build();
	}
	
	/**
	 * Delete a UserProfile 
	 * (and what else?)
	 * 
	 * @param id The vertex id of the UserProfile to delete
	 * @return The response
	 */
	@DELETE
	@Path("/userProfile/{id}")
	public Response deleteUserProfile(@PathParam("id") long id) {
		Views<UserProfile> views = new Views<UserProfile>(graph,
				UserProfile.class);

		try {
			views.delete(id, getRequesterUserProfileId());
			return Response.status(Status.OK).build();
		} catch (PermissionDenied e) {
			return Response.status(Status.UNAUTHORIZED)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (ValidationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		} catch (SerializationError e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity((produceErrorMessageJson(e)).getBytes()).build();
		}
	}
	
	
	
	/*** helpers ***/

	/**
	 * Retrieve the id of the UserProfile of the requester
	 * 
	 * @return The id
	 * @throws PermissionDenied
	 */
	private Long getRequesterUserProfileId() throws PermissionDenied {
		Long id;
		List<String> list = requestHeaders.getRequestHeader(AUTH_HEADER_NAME);

		if (list.isEmpty()) {
			throw new PermissionDenied("Authorization id missing");
		} else {
			// just take the first one and get the Long value
			try {
				id = Long.parseLong(list.get(0));
			} catch (NumberFormatException e) {
				throw new PermissionDenied("Authorization id has wrong format");
			}
		}

		return id;
	}

	/**
	 * Produce json formatted ErrorMessage
	 * 
	 * @param e The exception
	 * @return The json string
	 */
	private String produceErrorMessageJson(Exception e) {
		// NOTE only put in a stacktrace when debugging??
		// or no stacktraces, only by logging!

		String message = "{errormessage: \"  " + e.getMessage() + "\""
				+ ", stacktrace:  \"  " + getStackTrace(e) + "\"" + "}";

		return message;
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