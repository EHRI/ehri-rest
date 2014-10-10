package eu.ehri.extension.base;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.*;

import javax.ws.rs.core.Response;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface DeleteResource {
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
    public Response delete(String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError;
}
