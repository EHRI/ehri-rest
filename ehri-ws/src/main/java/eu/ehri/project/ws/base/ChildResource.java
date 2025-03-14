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

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;

import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Methods for managing resources that have a subordinate (hierarchical)
 * relationship to another resource.
 */
public interface ChildResource {

    /**
     * Set (or change) this item's parent. Depending on the item type only
     * one parent or multiple can be set.
     * <p>
     * Example:
     * <pre>
     *     <code>
     * curl http://localhost:7474/ehri/[RESOURCE]/[ID]/parent?id=[PARENT-ID]
     *     </code>
     * </pre>
     *
     * @return the reparented item
     * @throws ItemNotFound         when one of the provided IDs is invalid
     * @throws PermissionDenied     when the user does not have permission to modify
     *                              the provided items
     * @throws DeserializationError when a condition is not met, such as the parent
     *                              item(s) being of the wrong type
     */
    Response setParents(String id, List<String> parentIds)
            throws PermissionDenied, ItemNotFound, DeserializationError;
}
