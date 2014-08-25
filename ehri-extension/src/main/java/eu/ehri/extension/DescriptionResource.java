package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.views.DescriptionViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides a RESTful interface for dealing with described entities.
 */
@Path("description")
public class DescriptionResource extends AbstractAccessibleEntityResource<DescribedEntity> {

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
            throws PermissionDenied, ValidationError, IntegrityError,
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
            throws PermissionDenied, ValidationError, IntegrityError,
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
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester, SerializationError {
        // FIXME: Inefficient conversion to/from JSON just to insert the ID. We
        // should rethink this somehow.
        return updateDescription(id, bundle.withId(did));
    }

    @DELETE
    @Path("/{id:.+}/{did:.+}")
    public Response deleteDocumentaryUnitDescription(
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
        try {
            return Response.status(status).location(getItemUri(item))
                    .entity((getSerializer().vertexFrameToJson(data)).getBytes()).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/{did:.+}/" + Entities.UNDETERMINED_RELATIONSHIP)
    public Response createAccessPoint(@PathParam("id") String id,
                @PathParam("did") String did, Bundle bundle)
            throws PermissionDenied, ValidationError, IntegrityError,
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

    @DELETE
    @Path("/{id:.+}/{did:.+}/" + Entities.UNDETERMINED_RELATIONSHIP + "/{apid:.+}")
    public Response deleteAccessPoint(@PathParam("id") String id,
            @PathParam("did") String did, @PathParam("apid") String apid)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            DescribedEntity doc = views.detail(id, getRequesterUserProfile());
            Description desc = manager.getFrame(did, Description.class);
            if (!doc.equals(desc.getDescribedEntity())) {
                throw new PermissionDenied("Description does not belong to given entity.");
            }
            UndeterminedRelationship rel = manager.getFrame(apid, UndeterminedRelationship.class);
            if (!rel.getDescription().equals(desc)) {
                throw new PermissionDenied("Access point does not belong to given description.");
            }
            // FIXME: This could definitely be better
            new BundleDAO(graph).delete(getSerializer().vertexFrameToBundle(rel));
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
