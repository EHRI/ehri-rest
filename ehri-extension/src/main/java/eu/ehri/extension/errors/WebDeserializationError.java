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

package eu.ehri.extension.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import eu.ehri.project.exceptions.DeserializationError;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;


/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class WebDeserializationError extends WebApplicationException {
    private static final ObjectMapper mapper = new ObjectMapper();

    public WebDeserializationError(DeserializationError e) {
        super(Response.status(Status.BAD_REQUEST)
                .entity(errorToJson(e).getBytes(Charsets.UTF_8)).build());
    }

    public static String errorToJson(final DeserializationError e) {
        try {
            return mapper.writeValueAsString(new HashMap<String, Object>() {
                {
                    put("error", DeserializationError.class.getSimpleName());
                    put("details", e.getMessage());
                }
            });
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
