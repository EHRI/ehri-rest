/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
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
