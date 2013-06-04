package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.DescribedEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.persistance.Bundle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

/**
 * Provides a RESTfull interface for dealing with described entities.
 */
@Path("description")
public class DescriptionResource extends AbstractAccessibleEntityResource<DescribedEntity> {

    public DescriptionResource(@Context GraphDatabaseService database) {
        super(database, DescribedEntity.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response createDescription(@PathParam("id") String id, String json)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            DescribedEntity doc = views.detail(
                    manager.getFrame(id, DescribedEntity.class), user);
            Description desc = views.createDependent(Bundle.fromString(json),
                    doc, user, Description.class, getLogMessage());
            doc.addDescription(desc);
            tx.success();
            return buildResponse(desc, Response.Status.CREATED);
        } catch (SerializationError e) {
            tx.failure();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}")
    public Response updateDescription(@PathParam("id") String id, String json)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester, SerializationError {
        Accessor user = getRequesterUserProfile();
        DescribedEntity doc = views.detail(
                manager.getFrame(id, DescribedEntity.class), user);
        Description desc = views.updateDependent(Bundle.fromString(json), doc, user, Description.class,
                getLogMessage());
        return buildResponse(desc, Response.Status.OK);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        Accessor user = getRequesterUserProfile();
        DescribedEntity doc = views.detail(manager.getFrame(id, DescribedEntity.class), user);
        Description desc = manager.getFrame(did, EntityClass.DOCUMENT_DESCRIPTION,
                Description.class);
        views.deleteDependent(desc, doc, user, Description.class, getLogMessage());
        return Response.ok().build();
    }


    private Response buildResponse(Frame doc, Response.Status status)
            throws SerializationError {
        try {
            return Response.status(status)
                    .entity((serializer.vertexFrameToJson(doc)).getBytes()).build();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id:.+}/{did:.+}/" + Entities.UNDETERMINED_RELATIONSHIP)
    public Response createAccessPoint(@PathParam("id") String id,
                @PathParam("did") String did, String json)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            DescribedEntity doc = views.detail(
                    manager.getFrame(id, DescribedEntity.class), user);
            Description desc = manager.getFrame(did, Description.class);
            UndeterminedRelationship rel = views.createDependent(Bundle.fromString(json),
                    doc, user, UndeterminedRelationship.class, getLogMessage());
            desc.addUndeterminedRelationship(rel);
            tx.success();
            return buildResponse(rel, Response.Status.CREATED);
        } catch (SerializationError e) {
            tx.failure();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
    }

    @DELETE
    @Path("/{id:.+}/{did:.+}/" + Entities.UNDETERMINED_RELATIONSHIP + "/{apid:.+}")
    public Response deleteAccessPoint(@PathParam("id") String id,
            @PathParam("did") String did, @PathParam("did") String apid, String json)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            DescribedEntity doc = views.detail(
                    manager.getFrame(id, DescribedEntity.class), user);
            Description desc = manager.getFrame(did, Description.class);
            UndeterminedRelationship rel = manager.getFrame(apid, UndeterminedRelationship.class);
            if (!rel.getDescription().equals(desc)) {
                throw new WebApplicationException(
                        new Exception("Access point does not belong to given description."));
            }
            views.deleteDependent(rel, doc, user, UndeterminedRelationship.class, getLogMessage());
            return Response.ok().build();
        } catch (SerializationError e) {
            tx.failure();
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }
    }
}
