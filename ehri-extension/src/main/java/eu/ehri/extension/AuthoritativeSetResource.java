package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.cvoc.AuthoritativeItem;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.Query;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.List;

/**
 * Provides a RESTfull interface for the AuthoritativeSet Also for managing the
 * HistoricalAgents that are in the AuthoritativeSet
 * 
 * @author mikeb
 * 
 */
@Path(Entities.AUTHORITATIVE_SET)
public class AuthoritativeSetResource extends
        AbstractAccessibleEntityResource<AuthoritativeSet> {

    public AuthoritativeSetResource(@Context GraphDatabaseService database) {
        super(database, AuthoritativeSet.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getAuthoritativeSet(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public StreamingOutput listAuthoritativeSets(
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
    public Response countAuthoritativeSets(@QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return count(filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/page")
    public StreamingOutput pageAuthoritativeSets(
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
    public StreamingOutput listAuthoritativeSetHistoricalAgents(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        AuthoritativeSet set = views.detail(manager.getFrame(id, cls), user);
        Query<AuthoritativeItem> query = new Query<AuthoritativeItem>(graph, AuthoritativeItem.class)
                .setLimit(limit).setOffset(offset).orderBy(order)
                .filter(filters);
        return streamingList(query.list(set.getAuthoritativeItems(), user));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public Response countAuthoritativeSetHistoricalAgents(
            @PathParam("id") String id,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        AuthoritativeSet set = views.detail(manager.getFrame(id, cls), user);
        Query<AuthoritativeItem> query = new Query<AuthoritativeItem>(graph, AuthoritativeItem.class)
                .filter(filters);
        return Response.ok((query.count(set.getAuthoritativeItems(), user))
                .toString().getBytes()).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/page")
    public StreamingOutput pageAuthoritativeSetHistoricalAgents(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        AuthoritativeSet set = views.detail(manager.getFrame(id, cls), user);
        Query<AuthoritativeItem> query = new Query<AuthoritativeItem>(graph, AuthoritativeItem.class)
                .setLimit(limit).setOffset(offset).orderBy(order)
                .filter(filters);
        return streamingPage(query.page(set.getAuthoritativeItems(), user));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response createAuthoritativeSet(String json,
            @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        return create(json, accessors);
    }

    // Note: json contains id
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateAuthoritativeSet(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateAuthoritativeSet(@PathParam("id") String id, String json)
            throws AccessDenied, PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteAuthoritativeSet(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }

    /*** HistoricalAgent manipulation ***/

    @DELETE
    @Path("/{id:.+}/all")
    public Response deleteAllAuthoritativeSetHistoricalAgents(
            @PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied, PermissionDenied {
        AuthoritativeSet set = new Query<AuthoritativeSet>(graph, AuthoritativeSet.class).get(id,
                getRequesterUserProfile());
        try {
        	LoggingCrudViews<AuthoritativeItem> agentViews = new LoggingCrudViews<AuthoritativeItem>(graph,
                    AuthoritativeItem.class, set);
        	Accessor requesterUserProfile = getRequesterUserProfile();
        	Iterable<AuthoritativeItem> agents = set.getAuthoritativeItems();
        	for (AuthoritativeItem agent : agents) {
        		agentViews.delete(agent, requesterUserProfile);
        	}
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        } catch (ValidationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } catch (SerializationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Create a top-level agent unit for this set.
     *
     * @param id
     * @param json
     * @return
     * @throws eu.ehri.project.exceptions.PermissionDenied
     * @throws eu.ehri.project.exceptions.ValidationError
     * @throws eu.ehri.project.exceptions.IntegrityError
     * @throws eu.ehri.project.exceptions.DeserializationError
     * @throws eu.ehri.project.exceptions.ItemNotFound
     * @throws eu.ehri.extension.errors.BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.HISTORICAL_AGENT)
    public Response createAuthoritativeSetHistoricalAgent(@PathParam("id") String id,
            String json, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        AuthoritativeSet set = views.detail(manager.getFrame(id, cls), user);
        try {
            HistoricalAgent agent = createHistoricalAgent(json, set);
            new AclManager(graph).setAccessors(agent,
                    getAccessors(accessors, user));
            graph.getBaseGraph().commit();
            return buildResponseFromHistoricalAgent(agent);
        } catch (SerializationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } finally {
            cleanupTransaction();
        }
    }

    // Helpers

    private Response buildResponseFromHistoricalAgent(HistoricalAgent agent)
            throws SerializationError {
        String jsonStr = getSerializer().vertexFrameToJson(agent);
        // FIXME: Hide the details of building this path
        URI docUri = UriBuilder.fromUri(uriInfo.getBaseUri())
                .segment(Entities.HISTORICAL_AGENT)
                .segment(agent.getId())
                .build();

        return Response.status(Status.CREATED).location(docUri)
                .entity((jsonStr).getBytes()).build();
    }

    private HistoricalAgent createHistoricalAgent(String json, AuthoritativeSet set)
            throws DeserializationError, PermissionDenied, ValidationError,
            IntegrityError, BadRequester {
        Bundle entityBundle = Bundle.fromString(json);

        HistoricalAgent agent = new LoggingCrudViews<HistoricalAgent>(graph, HistoricalAgent.class,
                set).create(entityBundle, getRequesterUserProfile(),
                getLogMessage());

        // Add it to this AuthoritativeSet's agents
        agent.setAuthoritativeSet(set);
        return agent;
    }
}
