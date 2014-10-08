package eu.ehri.extension.base;

import eu.ehri.extension.errors.BadRequester;

import javax.ws.rs.core.Response;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface ListResource {
    /**
     * List available resources.
     *
     * @return A list of serialized item representations
     * @throws BadRequester
     */
    Response list() throws BadRequester;

    /**
     * Count the number of available resources.
     *
     * @return The total number of applicable items
     * @throws BadRequester
     */
    long count() throws BadRequester;
}
