package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.Query;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides a RESTful interface for the DocumentaryUnit
 */
@Path(Entities.DOCUMENTARY_UNIT)
public class DocumentaryUnitResource extends
        AbstractAccessibleEntityResource<DocumentaryUnit> {

    public DocumentaryUnitResource(@Context GraphDatabaseService database) {
        super(database, DocumentaryUnit.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getDocumentaryUnitJson(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public Response listDocumentaryUnits()
            throws ItemNotFound, BadRequester {
        return page();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    public Response countDocumentaryUnits() throws ItemNotFound, BadRequester {
        return count();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    public Response listChildDocumentaryUnits(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied {
        DocumentaryUnit parent = manager.getFrame(id, DocumentaryUnit.class);
        Iterable<DocumentaryUnit> units = all
                ? parent.getAllChildren()
                : parent.getChildren();
        return streamingPage(getQuery(cls).page(units, getRequesterUserProfile()));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public Response countChildDocumentaryUnits(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied {
        DocumentaryUnit parent = manager.getFrame(id, DocumentaryUnit.class);
        Iterable<DocumentaryUnit> units = all
                ? parent.getAllChildren()
                : parent.getChildren();
        return Response.ok((getQuery(cls).count(units,
                getRequesterUserProfile())).toString().getBytes()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateDocumentaryUnit(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateDocumentaryUnit(@PathParam("id") String id,
            String json) throws AccessDenied, PermissionDenied, IntegrityError,
            ValidationError, DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteDocumentaryUnit(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        return delete(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.DOCUMENTARY_UNIT)
    public Response createChildDocumentaryUnit(@PathParam("id") String id,
            String json, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        DocumentaryUnit parent = views.detail(id, user);
        try {
            DocumentaryUnit doc = createDocumentaryUnit(json, parent);
            aclManager.setAccessors(doc,
                    getAccessors(accessors, user));
            graph.getBaseGraph().commit();
            return creationResponse(doc);
        } catch (SerializationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } finally {
            cleanupTransaction();
        }
    }

    // Helpers

    private DocumentaryUnit createDocumentaryUnit(String json,
            DocumentaryUnit parent) throws DeserializationError,
            PermissionDenied, ValidationError, IntegrityError, BadRequester {
        Bundle entityBundle = Bundle.fromString(json);

        DocumentaryUnit doc = new LoggingCrudViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class, parent).create(entityBundle,
                getRequesterUserProfile(), getLogMessage());
        // NB: We no longer add this item to the
        // parent's Repository directly.
        parent.addChild(doc);
        return doc;
    }
}
