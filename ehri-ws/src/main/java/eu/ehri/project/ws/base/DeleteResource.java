/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
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

import eu.ehri.project.exceptions.*;


public interface DeleteResource {
    /**
     * Delete a resource.
     *
     * @param id The resource ID.
     * @throws ItemNotFound     if the parent does not exist
     * @throws PermissionDenied if the user cannot perform the action
     * @throws ValidationError  if data constraints are not met
     * @throws HierarchyError   if the action would result in orphaned child items
     */
    void delete(String id)
            throws PermissionDenied, ItemNotFound, ValidationError, HierarchyError;
}
