package eu.ehri.extension.base;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.ItemNotFound;

import javax.ws.rs.core.Response;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface GetResource {

    /**
     * Fetch a resource by id.
     *
     * @param id The requested item id
     * @return A serialized item representation
     * @throws ItemNotFound
     * @throws AccessDenied
     * @throws BadRequester
     */
    public Response get(String id)
            throws ItemNotFound, AccessDenied, BadRequester;

}
