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

package eu.ehri.extension.errors.mappers;

import com.google.common.base.Charsets;
import eu.ehri.project.exceptions.ValidationError;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
 * Serialize a tree of validation errors to JSON. Like bundles,
 * ValidationErrors are a recursive structure with a 'relations'
 * map that contains lists of the errors found in each top-level
 * item's children. The end result should look like:
 * <p>
 * {
 * "errors":{},
 * "relations":{
 * "describes":[
 * {}
 * ],
 * "hasDate":[
 * {
 * "errors":{
 * "startDate":["Missing mandatory field"],
 * "endDate":["Missing mandatory field"]
 * },
 * "relations":{}
 * }
 * ]
 * }
 * }
 * <p>
 * The response is sent with HTTP status Bad Request.
 */
@Provider
public class ValidationErrorMapper implements ExceptionMapper<ValidationError> {
    @Override
    public Response toResponse(ValidationError e) {
        try {
            return Response.status(Status.BAD_REQUEST)
                    .entity(e.getErrorSet().toJson()
                            .getBytes(Charsets.UTF_8)).build();
        } catch (Exception e1) {
            e1.printStackTrace();
            throw new RuntimeException(e1);
        }
    }
}
