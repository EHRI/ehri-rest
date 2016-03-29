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

package eu.ehri.extension.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.GlobalPermissionSet;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.exceptions.DeserializationError;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class GlobalPermissionSetProvider implements MessageBodyReader<GlobalPermissionSet>, JsonMessageBodyHandler {

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return aClass == GlobalPermissionSet.class;
    }

    @Override
    public GlobalPermissionSet readFrom(Class<GlobalPermissionSet> bundleClass, Type type, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String,
            String> headers, InputStream stream) throws IOException, WebApplicationException {

        try {
            return parseMatrix(stream);
        } catch (DeserializationError deserializationError) {
            throw new WebApplicationException(deserializationError, Response.Status.BAD_REQUEST);
        }
    }

    private GlobalPermissionSet parseMatrix(InputStream json) throws DeserializationError {
        try {
            TypeReference<HashMap<ContentTypes, List<PermissionType>>> typeRef = new TypeReference<HashMap<ContentTypes,
                    List<PermissionType>>>() {
            };
            HashMap<ContentTypes, Collection<PermissionType>> globals = mapper.readValue(json, typeRef);
            return GlobalPermissionSet.from(globals);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        } catch (IOException e) {
            throw new DeserializationError(e.getMessage());
        }
    }
}
