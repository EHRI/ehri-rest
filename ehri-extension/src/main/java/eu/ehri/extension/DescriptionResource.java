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
import javax.ws.rs.core.*;

/**
 * Provides a RESTful interface for dealing with described entities.
 */
@Path("description")
public class DescriptionResource extends AbstractAccessibleEntityResource<DescribedEntity> {

    private final DescriptionViews<DescribedEntity> descriptionViews;

    public DescriptionResource(@Context GraphDatabaseService database, @Context HttpHeaders requestHeaders) {
        super(database, requestHeaders, DescribedEntity.class);
        descriptionViews = new DescriptionViews<DescribedEntity>(graph, DescribedEntity.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response createDescription(@PathParam("id") String id, String json)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        checkNotInTransaction();
        Accessor user = getRequesterUserProfile();
        try {
            DescribedEntity doc = views.detail(id, user);
            Description desc = descriptionViews.create(id, Bundle.fromString(json),
                    Description.class, user, getLogMessage());
            doc.addDescription(desc);
            graph.getBaseGraph().commit();
            return buildResponse(desc, Response.Status.CREATED);
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
    public Response updateDescription(@PathParam("id") String id, String json)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester, SerializationError {
        checkNotInTransaction();
        try {
            Mutation<Description> desc = descriptionViews.update(id, Bundle.fromString(json),
                    Description.class, getRequesterUserProfile(), getLogMessage());
            graph.getBaseGraph().commit();
            return buildResponse(desc.getNode(), Response.Status.OK);
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
            @PathParam("did") String did, String json)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester, SerializationError {
        // FIXME: Inefficient conversion to/from JSON just to insert the ID. We
        // should rethink this somehow.
        return updateDescription(id, Bundle.fromString(json).withId(did).toJson());
    }

    @DELETE
    @Path("/{id:.+}/{did:.+}")
    public Response deleteDocumentaryUnitDescription(
            @PathParam("id") String id, @PathParam("did") String did)
            throws PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        checkNotInTransaction();
        try {
            descriptionViews.delete(id, did, getRequesterUserProfile(), getLogMessage());
            graph.getBaseGraph().commit();
            return Response.ok().build();
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }


    private Response buildResponse(Frame doc, Response.Status status)
            throws SerializationError {
        try {
            return Response.status(status)
                    .entity((getSerializer().vertexFrameToJson(doc)).getBytes()).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/{did:.+}/" + Entities.UNDETERMINED_RELATIONSHIP)
    public Response createAccessPoint(@PathParam("id") String id,
                @PathParam("did") String did, String json)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        checkNotInTransaction();
        try {
            Accessor user = getRequesterUserProfile();
            Description desc = manager.getFrame(did, Description.class);
            UndeterminedRelationship rel = descriptionViews.create(id, Bundle.fromString(json),
                    UndeterminedRelationship.class, user, getLogMessage());
            desc.addUndeterminedRelationship(rel);
            graph.getBaseGraph().commit();
            return buildResponse(rel, Response.Status.CREATED);
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
        checkNotInTransaction();
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
