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

import eu.ehri.project.exceptions.AccessDenied;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class AccessDeniedMapper implements ExceptionMapper<AccessDenied> {

	@SuppressWarnings("serial")
    @Override
	public Response toResponse(final AccessDenied e) {
        Map<String, Object> out = new HashMap<String, Object>() {
            {
                put("error", AccessDenied.class.getSimpleName());
                put("details", new HashMap<String, String>() {
                    {
                        put("message", e.getMessage());
                        put("item", e.getEntity());
                        put("accessor", e.getAccessor());
                    }
                });
            }
        };
		try {
            return Response.status(Status.UNAUTHORIZED)
            	.entity(new ObjectMapper().writeValueAsBytes(out)).build();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
	}
}
