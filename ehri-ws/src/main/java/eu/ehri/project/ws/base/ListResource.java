/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.ws.base;

import javax.ws.rs.core.Response;


public interface ListResource {

    /**
     * List available resources. The default behaviour is to
     * return a page of 20 items.
     * <p>
     * The default list limit can be overridden using the <code>limit</code>
     * parameter, and disabled completely using a value of -1.
     * <p>
     * Item lists are paginated using the <code>page</code> parameter.
     * <p>
     * The page, limit, and total number of items is returned in the Content-Range response
     * header in the form:
     * <pre><code>page=1; count=20; total=50</code></pre>
     * <p>
     * If the header <code>X-Stream</code> is set to <code>true</code>
     * no count of total items will be performed. This is more efficient when returning large
     * numbers of items or when limiting is disabled completely.
     * <p>
     * Example:
     * <pre>
     *      <code>
     * curl http://localhost:7474/ehri/[RESOURCE]/list?page=5&amp;limit=10
     *      </code>
     * </pre>
     *
     * @return A list of serialized item representations
     */
    Response list();
}
