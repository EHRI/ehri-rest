package eu.ehri.extension.base;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.persistence.Bundle;

import javax.ws.rs.core.Response;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface UpdateResource {

    /**
     * Update a resource.
     *
     * @param bundle A resource bundle, including its ID.
     * @return A serialized representation of the updated resource.
     * @throws eu.ehri.project.exceptions.PermissionDenied
     * @throws eu.ehri.project.exceptions.ValidationError
     * @throws eu.ehri.project.exceptions.DeserializationError
     * @throws eu.ehri.project.exceptions.ItemNotFound
     * @throws eu.ehri.extension.errors.BadRequester
     */
    public Response update(Bundle bundle) throws PermissionDenied,
            ValidationError, DeserializationError,
            ItemNotFound, BadRequester;

    /**
     * Update a resource.
     *
     * @param id     The resource ID.
     * @param bundle A resource bundle, with or without an ID.
     * @return A serialized representation of the updated resource.
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response update(String id, Bundle bundle)
                throws AccessDenied, PermissionDenied,
            ValidationError, DeserializationError, ItemNotFound, BadRequester;
}
