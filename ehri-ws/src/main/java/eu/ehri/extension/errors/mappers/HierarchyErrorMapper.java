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

package eu.ehri.extension.errors.mappers;

import com.google.common.collect.ImmutableMap;
import eu.ehri.extension.errors.WebDeserializationError;
import eu.ehri.project.exceptions.HierarchyError;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps the {@link HierarchyError} exception to the Precondition Failed response.
 */
@Provider
public class HierarchyErrorMapper implements ExceptionMapper<HierarchyError> {
    @Override
    public Response toResponse(final HierarchyError e) {
        return WebDeserializationError.errorToJson(
                Status.CONFLICT,
                ImmutableMap.of(
                        "message", e.getMessage(),
                        "id", e.id(),
                        "count", e.childCount()
                )
        );
    }
}