package eu.ehri.extension.providers;

import eu.ehri.extension.errors.WebDeserializationError;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.persistence.Bundle;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class BundleProvider implements MessageBodyReader<Bundle> {
    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return aClass == Bundle.class;
    }

    @Override
    public Bundle readFrom(Class<Bundle> bundleClass, Type type, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String,
            String> headers, InputStream stream) throws IOException, WebApplicationException {

        try {
            return Bundle.fromStream(stream);
        } catch (DeserializationError deserializationError) {
            throw new WebDeserializationError(deserializationError);
        }
    }
}
