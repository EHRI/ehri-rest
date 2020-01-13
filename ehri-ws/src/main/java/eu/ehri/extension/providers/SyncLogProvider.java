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

package eu.ehri.extension.providers;

import eu.ehri.project.importers.ead.SyncLog;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
public class SyncLogProvider implements MessageBodyWriter<SyncLog>,
        MessageBodyReader<SyncLog>, JsonMessageBodyHandler {
    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return aClass == SyncLog.class;
    }

    @Override
    public long getSize(SyncLog syncLog, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return -1L;
    }

    @Override
    public void writeTo(SyncLog syncLog, Class<?> aClass, Type type,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream)
            throws IOException, WebApplicationException {
        if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
            mapper.writeValue(outputStream, syncLog);
        } else {
            new PrintStream(outputStream).println(syncLog);
        }
    }

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return aClass == SyncLog.class;
    }

    @Override
    public SyncLog readFrom(Class<SyncLog> aClass, Type type,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
            throws IOException, WebApplicationException {
        return mapper.readValue(inputStream, SyncLog.class);
    }
}
