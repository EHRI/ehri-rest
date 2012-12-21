package eu.ehri.extension;

import java.io.IOException;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import com.google.common.collect.Lists;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.impl.AclViews;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.impl.Query;

/**
 * Provides a RESTfull interface for the Agent
 */
@Path(Entities.AGENT)
public class AgentResource extends EhriNeo4jFramedResource<Agent> {

    public AgentResource(@Context GraphDatabaseService database) {
        super(database, Agent.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:\\d+}")
    public Response getAgent(@PathParam("id") long id) throws PermissionDenied,
            BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response getAgent(@PathParam("id") String id) throws ItemNotFound,
            PermissionDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public StreamingOutput listAgents(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit)
            throws ItemNotFound, BadRequester {
        return list(offset, limit);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}/list")
    public StreamingOutput listAgentDocumentaryUnits(
            @PathParam("id") String id,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit)
            throws ItemNotFound, BadRequester, PermissionDenied {
        Agent agent = new Query<Agent>(graph, Agent.class).get(id,
                getRequesterUserProfile());
        return list(agent.getCollections(), offset, limit);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/page")
    public StreamingOutput pageAgents(
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit)
            throws ItemNotFound, BadRequester {
        return page(offset, limit);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAgent(String json) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {
        return create(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAgent(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response updateAgent(@PathParam("id") String id, String json)
            throws PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteAgent(@PathParam("id") long id)
            throws PermissionDenied, ValidationError, ItemNotFound,
            BadRequester {
        return delete(id);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteAgent(@PathParam("id") String id)
            throws PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }

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
    public Response createAgentDocumentaryUnit(@PathParam("id") long id,
            String json) throws PermissionDenied, ValidationError,
            IntegrityError, DeserializationError, SerializationError,
            BadRequester {
        Agent agent = views.detail(graph.getVertex(id, cls),
                getRequesterUserProfile());
        DocumentaryUnit doc = createDocumentaryUnit(json, agent);
        return buildResponseFromDocumentaryUnit(doc);
    }

    /**
     * Set a user's documentary unit permissions for this repository scope.
     * 
     * FIXME: Generalise this behaviour.
     * 
     * @param id
     * @param json
     * @return
     * @throws PermissionDenied
     * @throws IOException
     * @throws ItemNotFound
     * @throws DeserializationError
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}/grant/{userId:.+}")
    public Response setDocumentaryUnitPermissions(@PathParam("id") String id,
            @PathParam("userId") String userId, String json)
            throws PermissionDenied, IOException, ItemNotFound,
            DeserializationError, BadRequester {

        List<String> scopedPerms;
        try {
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {
            };
            scopedPerms = mapper.readValue(json, typeRef);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        }

        Agent agent = manager.getFrame(id, Agent.class);
        Accessor accessor = manager.getFrame(userId, Accessor.class);
        Accessor grantee = getRequesterUserProfile();
        AclViews<Agent> acl = new AclViews<Agent>(graph,
                Agent.class, agent);
        acl.setScopedPermissions(accessor, grantee,
                ContentTypes.DOCUMENTARY_UNIT, enumifyPermissionList(scopedPerms));
        return Response.status(Status.OK).build();
    }

    private List<PermissionType> enumifyPermissionList(List<String> scopedPerms)
        throws DeserializationError {
        try {
            List<PermissionType> perms = Lists.newLinkedList();
            for (String p : scopedPerms) {
                perms.add(PermissionType.withName(p));
            }
            
            return perms;
        } catch (IllegalArgumentException e) {
            throw new DeserializationError(e.getMessage());
        }
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
    @Path("/{id:.+}/" + Entities.DOCUMENTARY_UNIT)
    public Response createAgentDocumentaryUnit(@PathParam("id") String id,
            String json) throws PermissionDenied, ValidationError,
            IntegrityError, DeserializationError, ItemNotFound, BadRequester {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Agent agent = new Query<Agent>(graph, Agent.class).get(id,
                    getRequesterUserProfile());
            DocumentaryUnit doc = createDocumentaryUnit(json, agent);
            tx.success();
            return buildResponseFromDocumentaryUnit(doc);
        } catch (SerializationError e) {
            tx.failure();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
    }

    // Helpers

    private Response buildResponseFromDocumentaryUnit(DocumentaryUnit doc)
            throws SerializationError {
        String jsonStr = converter.vertexFrameToJson(doc);
        // FIXME: Hide the details of building this path
        URI docUri = UriBuilder.fromUri(uriInfo.getBaseUri())
                .segment(Entities.DOCUMENTARY_UNIT).segment(manager.getId(doc))
                .build();

        return Response.status(Status.CREATED).location(docUri)
                .entity((jsonStr).getBytes()).build();
    }

    private DocumentaryUnit createDocumentaryUnit(String json, Agent agent)
            throws DeserializationError, PermissionDenied, ValidationError,
            IntegrityError, BadRequester {
        Bundle entityBundle = converter.jsonToBundle(json);

        DocumentaryUnit doc = new LoggingCrudViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class, agent)
                .create(converter.bundleToData(entityBundle),
                        getRequesterUserProfile());
        // Add it to this agent's collections
        doc.setAgent(agent);
        doc.setScope(agent);
        return doc;
    }
}
