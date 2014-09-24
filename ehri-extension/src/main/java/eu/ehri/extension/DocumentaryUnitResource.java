package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
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

    /**
     * Fetch a documentary unit by id.
     *
     * @param id The requested item id
     * @return A serialized item representation
     * @throws ItemNotFound
     * @throws AccessDenied
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getDocumentaryUnitJson(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return retrieve(id);
    }

    /**
     * List available documentary units.
     *
     * @return A list of serialized item representations
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public Response listDocumentaryUnits()
            throws ItemNotFound, BadRequester {
        return page();
    }

    /**
     * Count the number of available documentary units.
     *
     * @return The total number of applicable items
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    public long countDocumentaryUnits() throws ItemNotFound, BadRequester {
        return count();
    }

    /**
     * List the child documentary units held by this item.
     *
     * @param id  The requested item id
     * @param all Whether to list all child items, or just those at the
     *            immediate sub-level.
     * @return A list of serialized item representations
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
     */
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

    /**
     * Count the number of available documentary units held by this item.
     *
     * @param id  The requested item id
     * @param all Whether to count all child items, or just those at the
     *            immediate sub-level.
     * @return The total number of applicable items
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public long countChildDocumentaryUnits(
            @PathParam("id") String id,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied {
        DocumentaryUnit parent = manager.getFrame(id, DocumentaryUnit.class);
        Iterable<DocumentaryUnit> units = all
                ? parent.getAllChildren()
                : parent.getChildren();
        return getQuery(cls).count(units);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateDocumentaryUnit(Bundle bundle) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(bundle);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateDocumentaryUnit(@PathParam("id") String id,
            Bundle bundle) throws AccessDenied, PermissionDenied, IntegrityError,
            ValidationError, DeserializationError, ItemNotFound, BadRequester {
        return update(id, bundle);
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
