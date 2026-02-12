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

package eu.ehri.project.ws.errors.mappers;

import eu.ehri.project.exceptions.RuntimeIntegrityError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.ws.errors.WebDeserializationError;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
 * Serialize an integrity error.
 */
@Provider
public class RuntimeIntegrityErrorMapper implements ExceptionMapper<RuntimeIntegrityError> {
    @Override
    public Response toResponse(RuntimeIntegrityError e) {
        return WebDeserializationError.errorToJson(
                Status.INTERNAL_SERVER_ERROR,
                e.getMessage(),
                "This can occur when attempting to import, patch, or update items that have been " +
                        "renamed (local identifiers changed).");
    }
}
