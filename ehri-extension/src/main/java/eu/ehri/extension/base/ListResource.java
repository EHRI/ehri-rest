package eu.ehri.extension.base;

import eu.ehri.extension.errors.BadRequester;

import javax.ws.rs.core.Response;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface ListResource {

    /**
     * List available resources. The default behaviour is to
     * return a page of 20 items.
     * <p/>
     * The default list limit can be overridden using the <code>limit</code>
     * parameter, and disabled completely using a value of -1.
     * <p/>
     * Item lists are paginated using the <code>page</code> parameter.
     * <p/>
     * The page, limit, and total number of items is returned in the Content-Range response
     * header in the form:
     *
     * <pre><code>page=1; count=20; total=50</code></pre>
     *
     * If the header <code>X-Stream</code> is set to <code>true</code>
     * no count of total items will be performed. This is more efficient when returning large
     * numbers of items or when limiting is disabled completely.
     * <p/>
     * Example:
     * <pre>
     *     <code>
     * curl http://localhost:7474/ehri/[RESOURCE]/list?page=5&limit=10
     *     </code>
     * </pre>
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
