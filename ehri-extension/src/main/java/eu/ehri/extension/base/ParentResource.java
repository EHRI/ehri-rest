package eu.ehri.extension.base;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.persistence.Bundle;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Methods for managing resources that have a subordinate (hierarchical)
 * relationship to another resource.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface ParentResource {

    /**
     * List available resources subordinate to this item. Behaviour is the same
     * as the general list method with respect to parameters.
     * <p/>
     * Example:
     * <pre>
     *     <code>
     * curl http://localhost:7474/ehri/[RESOURCE]/[ID]/list
     *     </code>
     * </pre>
     *
     * @return A list of serialized item representations
     * @throws BadRequester
     */
    public Response listChildren(String id, boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied;

    /**
     * Count the number of available resources subordinate to this item.
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
     * Create a subordinate resource.
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
