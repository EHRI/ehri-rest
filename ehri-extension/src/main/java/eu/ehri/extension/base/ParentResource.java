package eu.ehri.extension.base;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.persistence.Bundle;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface ParentResource {

    /**
     * List the child resources held by this item.
     *
     * @param id  The requested item id
     * @param all Whether to list all child items, or just those at the
     *            immediate sub-level.
     * @return A list of serialized item representations
     * @throws eu.ehri.project.exceptions.ItemNotFound
     * @throws eu.ehri.extension.errors.BadRequester
     * @throws eu.ehri.project.exceptions.PermissionDenied
     */
    public Response listChildren(String id, boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied;

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
    public long countChildren(String id, boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied;

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
    public Response createChild(String id,
                                Bundle bundle, List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester;
}
