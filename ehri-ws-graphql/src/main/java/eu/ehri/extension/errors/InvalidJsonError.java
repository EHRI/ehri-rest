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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Map;

public class InvalidJsonError extends WebApplicationException {
    private static final ObjectMapper mapper = new ObjectMapper();

    public InvalidJsonError(JsonProcessingException cause) {
        super(Response.status(Response.Status.BAD_REQUEST)
                .entity(errorToJson(cause).getBytes(Charsets.UTF_8)).build());
    }

    private static String errorToJson(JsonProcessingException cause) {
        try {
            Map<String, Object> error = ImmutableMap.of(
                    "message", cause.getOriginalMessage(),
                    "location", ImmutableList.of(ImmutableMap.of(
                            "line", cause.getLocation().getLineNr(),
                            "column", cause.getLocation().getColumnNr()
                    )),
                    "type", "JsonError"
            );
            return mapper.writeValueAsString(
                    ImmutableMap.of("errors", ImmutableList.of(error))
            );
        } catch (JsonProcessingException err) {
            throw new RuntimeException(err);
        }
    }
}
