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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import eu.ehri.extension.base.AbstractResource;
import eu.ehri.extension.errors.WebDeserializationError;
import eu.ehri.project.utils.Table;
import eu.ehri.project.exceptions.DeserializationError;

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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Provider for row/column data.
 * <p>
 * TODO: Handle CSV headers
 */
@Provider
@Produces({MediaType.APPLICATION_JSON, AbstractResource.CSV_MEDIA_TYPE})
@Consumes({MediaType.APPLICATION_JSON, AbstractResource.CSV_MEDIA_TYPE})
public class TableProvider implements MessageBodyWriter<Table>, MessageBodyReader<Table>, JsonMessageBodyHandler {

    private static CsvMapper csvMapper = new CsvMapper()
            .enable(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
            .enable(CsvParser.Feature.WRAP_AS_ARRAY);
    private static CsvSchema csvSchema = csvMapper.schemaFor(List.class);

    private static TypeReference<List<List<String>>> typeRef = new TypeReference<List<List<String>>>() {
    };

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Table.class.equals(type);
    }

    @Override
    public long getSize(Table table, Class<?> aClass, Type type, Annotation[] annotations,
            MediaType mediaType) {
        return -1L;
    }

    @Override
    public void writeTo(Table table,
            Class<?> aClass, Type type, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> headers,
            OutputStream outputStream) throws IOException, WebApplicationException {
        ObjectWriter writer = mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)
                ? mapper.writer()
                : csvMapper.writer(csvSchema.withoutHeader());
        writer.writeValue(outputStream, table.rows());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Table.class.equals(type);
    }

    @Override
    public Table readFrom(Class<Table> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            ObjectReader reader = mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)
                    ? mapper.readerFor(typeRef)
                    : csvMapper.readerFor(typeRef).with(csvSchema.withoutHeader());
            return Table.of(reader.readValue(entityStream));
        } catch (JsonMappingException | JsonParseException e) {
            throw new WebDeserializationError(new DeserializationError(
                    "Deserialization error for media type: " + mediaType, e));
        }
    }
}
