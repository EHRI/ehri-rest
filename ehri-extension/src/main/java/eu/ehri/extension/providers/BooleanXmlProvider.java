package eu.ehri.extension.providers;

import com.google.common.base.Charsets;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Provider
@Produces(MediaType.TEXT_XML)
public class BooleanXmlProvider implements MessageBodyWriter<Boolean> {

    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return Boolean.class.isAssignableFrom(aClass);
    }

    @Override
    public long getSize(Boolean bool, Class<?> aClass, Type type, Annotation[] annotations,
            MediaType mediaType) {
        return -1L;
    }

    @Override
    public void writeTo(Boolean bool, Class<?> aClass, Type type, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> headers,
            OutputStream outputStream) throws IOException, WebApplicationException {
        outputStream.write(("<boolean>" + bool + "</boolean>").getBytes(Charsets.UTF_8));
    }
}
