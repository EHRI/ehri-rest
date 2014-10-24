package eu.ehri.extension;

import eu.ehri.extension.base.CreateResource;
import eu.ehri.extension.base.DeleteResource;
import eu.ehri.extension.base.GetResource;
import eu.ehri.extension.base.ListResource;
import eu.ehri.extension.base.ParentResource;
import eu.ehri.extension.base.UpdateResource;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.cvoc.AuthoritativeItem;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.neo4j.graphdb.GraphDatabaseService;

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
import javax.ws.rs.core.Response.Status;
import java.util.List;

/**
 * Provides a web service interface for the AuthoritativeSet items. model.
 * Authoritative Sets are containers for Historical Agents
 * (authority files.)
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 * 
 */
@Path(Entities.AUTHORITATIVE_SET)
public class AuthoritativeSetResource extends
        AbstractAccessibleEntityResource<AuthoritativeSet>
            implements GetResource, ListResource, DeleteResource, CreateResource, UpdateResource, ParentResource {

    public AuthoritativeSetResource(@Context GraphDatabaseService database) {
        super(database, AuthoritativeSet.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    @Override
    public Response get(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return getItem(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    @Override
    public Response list() throws BadRequester {
        return listItems();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    @Override
    public long count() throws BadRequester {
        return countItems();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    @Override
    public Response listChildren(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        AuthoritativeSet set = views.detail(id, user);
        return streamingPage(getQuery(AuthoritativeItem.class)
                .page(set.getAuthoritativeItems(), user));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    @Override
    public long countChildren(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        AuthoritativeSet set = views.detail(id, user);
        return getQuery(AuthoritativeItem.class).count(set.getAuthoritativeItems());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Override
    public Response create(Bundle bundle,
                           @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        return createItem(bundle, accessors);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Override
    public Response update(Bundle bundle) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return updateItem(bundle);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    @Override
    public Response update(@PathParam("id") String id, Bundle bundle)
            throws AccessDenied, PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return updateItem(id, bundle);
    }

    @DELETE
    @Path("/{id:.+}")
    @Override
    public Response delete(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return deleteItem(id);
    }

    @DELETE
    @Path("/{id:.+}/all")
    public Response deleteAllAuthoritativeSetHistoricalAgents(
            @PathParam("id") String id)
            throws ItemNotFound, BadRequester, AccessDenied, PermissionDenied {
        Accessor user = getRequesterUserProfile();
        AuthoritativeSet set = views.detail(id, user);
        try {
        	LoggingCrudViews<AuthoritativeItem> agentViews = new LoggingCrudViews<AuthoritativeItem>(graph,
                    AuthoritativeItem.class, set);
        	Iterable<AuthoritativeItem> agents = set.getAuthoritativeItems();
        	for (AuthoritativeItem agent : agents) {
        		agentViews.delete(agent.getId(), user);
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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.HISTORICAL_AGENT)
    @Override
    public Response createChild(@PathParam("id") String id,
                                Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        try {
            Accessor user = getRequesterUserProfile();
            final AuthoritativeSet set = views.detail(id, user);
            return createItem(bundle, accessors, new Handler<HistoricalAgent>() {
                @Override
                public void process(HistoricalAgent agent) throws PermissionDenied {
                    set.addItem(agent);
                    agent.setPermissionScope(set);
                }
            }, views.setScope(set).setClass(HistoricalAgent.class));
        } finally {
            cleanupTransaction();
        }
    }
}
