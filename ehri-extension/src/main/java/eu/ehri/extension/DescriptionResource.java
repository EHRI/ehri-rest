package eu.ehri.extension;

import com.google.common.base.Charsets;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.views.DescriptionViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides a RESTful interface for dealing with described entities.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(DescriptionResource.ENDPOINT)
public class DescriptionResource extends AbstractAccessibleEntityResource<DescribedEntity> {

    public static final String ENDPOINT = "description";

    private final DescriptionViews<DescribedEntity> descriptionViews;

    public DescriptionResource(@Context GraphDatabaseService database) {
        super(database, DescribedEntity.class);
            descriptionViews = new DescriptionViews<DescribedEntity>(graph, DescribedEntity.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response createDescription(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor user = getRequesterUserProfile();
            DescribedEntity item = views.detail(id, user);
            Description desc = descriptionViews.create(id, bundle,
                    Description.class, user, getLogMessage());
            item.addDescription(desc);
            graph.getBaseGraph().commit();
            return buildResponse(item, desc, Response.Status.CREATED);
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateDescription(@PathParam("id") String id, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester, SerializationError {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor userProfile = getRequesterUserProfile();
            DescribedEntity item = views.detail(id, userProfile);
            Mutation<Description> desc = descriptionViews.update(id, bundle,
                    Description.class, userProfile, getLogMessage());
            graph.getBaseGraph().commit();
            return buildResponse(item, desc.getNode(), Response.Status.OK);
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/{did:.+}")
    public Response updateDescriptionWithId(@PathParam("id") String id,
            @PathParam("did") String did, Bundle bundle)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester, SerializationError {
        return updateDescription(id, bundle.withId(did));
    }

    @DELETE
    @Path("/{id:.+}/{did:.+}")
    public Response deleteDescription(
            @PathParam("id") String id, @PathParam("did") String did)
            throws PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor user = getRequesterUserProfile();
            DescribedEntity item = views.detail(id, user);
            descriptionViews.delete(id, did, user, getLogMessage());
            graph.getBaseGraph().commit();
            return Response.ok().location(getItemUri(item)).build();
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }


    private Response buildResponse(DescribedEntity item, Frame data, Response.Status status)
            throws SerializationError {
        return Response.status(status).location(getItemUri(item))
                .entity((getSerializer().vertexFrameToJson(data))
                        .getBytes(Charsets.UTF_8)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/{did:.+}/" + Entities.UNDETERMINED_RELATIONSHIP)
    public Response createAccessPoint(@PathParam("id") String id,
                @PathParam("did") String did, Bundle bundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor user = getRequesterUserProfile();
            DescribedEntity item = views.detail(id, user);
            Description desc = manager.getFrame(did, Description.class);
            UndeterminedRelationship rel = descriptionViews.create(id, bundle,
                    UndeterminedRelationship.class, user, getLogMessage());
            desc.addUndeterminedRelationship(rel);
            graph.getBaseGraph().commit();
            return buildResponse(item, rel, Response.Status.CREATED);
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }

    @SuppressWarnings("unused")
    @DELETE
    @Path("/{id:.+}/{did:.+}/" + Entities.UNDETERMINED_RELATIONSHIP + "/{apid:.+}")
    public Response deleteAccessPoint(@PathParam("id") String id,
            @PathParam("did") String did, @PathParam("apid") String apid)
            throws AccessDenied, PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            UserProfile user = getCurrentUser();
            descriptionViews.delete(id, apid, user, getLogMessage());
            graph.getBaseGraph().commit();
            return Response.ok().build();
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }
}
