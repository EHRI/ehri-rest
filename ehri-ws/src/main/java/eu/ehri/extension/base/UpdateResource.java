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

package eu.ehri.extension.base;

import eu.ehri.project.exceptions.*;
import eu.ehri.project.persistence.Bundle;

import javax.ws.rs.core.Response;


public interface UpdateResource {
    /**
     * Update a resource.
     *
     * @param id     The resource ID.
     * @param bundle A resource bundle, with or without an ID.
     * @return A serialized representation of the updated resource.
     * @throws ItemNotFound         if the item does not exist
     * @throws PermissionDenied     if the user cannot perform the action
     * @throws ValidationError      if data constraints are not met
     * @throws DeserializationError if the incoming data is badly-formed
     */
    Response update(String id, Bundle bundle)
            throws PermissionDenied,
            ValidationError, DeserializationError, ItemNotFound;
}
