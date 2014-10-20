package eu.ehri.extension.base;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.persistence.Bundle;

import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface CreateResource {

    /**
     * Create a resource.
     *
     * @param bundle    The resource data.
     * @param accessors The users/groups who can initially access this resource.
     * @return A serialized resource representation.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response create(Bundle bundle, List<String> accessors)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester;
}
