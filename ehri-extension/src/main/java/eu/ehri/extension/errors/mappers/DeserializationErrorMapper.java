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

import eu.ehri.extension.errors.WebDeserializationError;
import eu.ehri.project.exceptions.DeserializationError;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Maps the {@link DeserializationError} exception to the Bad Request response.
 *
 * @author Paul Boon (http://github.com/PaulBoon)
 * @author Mike Bryant (http://github.com/mikesname)
 *
 */
@Provider
public class DeserializationErrorMapper implements
        ExceptionMapper<DeserializationError> {

    @SuppressWarnings("unused")
    private String getStacktrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    @SuppressWarnings("serial")
    @Override
    public Response toResponse(final DeserializationError e) {
        return Response
                .status(Status.BAD_REQUEST)
                .entity(WebDeserializationError.errorToJson(e)
                        .getBytes(Charsets.UTF_8)).build();
    }
}
