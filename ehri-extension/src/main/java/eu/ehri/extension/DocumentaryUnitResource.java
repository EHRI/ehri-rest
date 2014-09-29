package eu.ehri.extension;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides a RESTful interface for the DocumentaryUnit
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Path(Entities.DOCUMENTARY_UNIT)
public class DocumentaryUnitResource extends
        AbstractAccessibleEntityResource<DocumentaryUnit> {

    public DocumentaryUnitResource(@Context GraphDatabaseService database) {
        super(database, DocumentaryUnit.class);
    }

    /**
     * Fetch a resource by id.
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
     * List available resources.
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
     * Count the number of available resources.
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
     * List the child resources held by this item.
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
     * Count the number of available resources held by this item.
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

    /**
     * Update a resource.
     *
     * @param bundle A resource bundle, including its ID.
     * @return A serialized representation of the updated resource.
     * @throws PermissionDenied
     * @throws IntegrityError
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateDocumentaryUnit(Bundle bundle) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(bundle);
    }

    /**
     * Update a resource.
     *
     * @param id     The resource ID.
     * @param bundle A resource bundle, with or without an ID.
     * @return A serialized representation of the updated resource.
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws IntegrityError
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateDocumentaryUnit(@PathParam("id") String id,
            Bundle bundle) throws AccessDenied, PermissionDenied, IntegrityError,
            ValidationError, DeserializationError, ItemNotFound, BadRequester {
        return update(id, bundle);
    }

    /**
     * Delete a resource.
     *
     * @param id The resource ID.
     * @return A response with 200 representing a successful deletion.
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     * @throws SerializationError
     */
    @DELETE
    @Path("/{id:.+}")
    public Response deleteDocumentaryUnit(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        return delete(id);
    }

    /**
     * Create a child resource.
     *
     * @param id The parent resource ID.
     * @param bundle A resource bundle.
     * @param accessors The users/groups who can access this item.
     * @return A serialized representation of the created resource.
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.DOCUMENTARY_UNIT)
    public Response createChildDocumentaryUnit(@PathParam("id") String id,
            Bundle bundle, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        try {
            Accessor user = getRequesterUserProfile();
            final DocumentaryUnit parent = views.detail(id, user);
            return create(bundle, accessors, new Handler<DocumentaryUnit>() {
                @Override
                public void process(DocumentaryUnit doc) throws PermissionDenied {
                    parent.addChild(doc);
                }
            });
        } finally {
            cleanupTransaction();
        }
    }
}
