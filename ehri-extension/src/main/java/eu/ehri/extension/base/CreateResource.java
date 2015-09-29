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
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
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
     * @throws DeserializationError
     * @throws BadRequester
     */
    Response create(Bundle bundle, List<String> accessors)
            throws PermissionDenied, ValidationError,
            DeserializationError, BadRequester;
}
