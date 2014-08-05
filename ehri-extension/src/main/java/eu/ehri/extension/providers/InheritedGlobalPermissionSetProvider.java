package eu.ehri.extension.providers;

import eu.ehri.project.acl.InheritedGlobalPermissionSet;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

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
@Produces(MediaType.APPLICATION_JSON)
public class InheritedGlobalPermissionSetProvider implements MessageBodyWriter<InheritedGlobalPermissionSet> {

    private static JsonFactory factory = new JsonFactory();
    private static ObjectMapper mapper = new ObjectMapper(factory);

    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return aClass == InheritedGlobalPermissionSet.class;
    }

    @Override
    public long getSize(InheritedGlobalPermissionSet globalPermissionSet, Class<?> aClass, Type type, Annotation[] annotations,
            MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(InheritedGlobalPermissionSet globalPermissionSet,
            Class<?> aClass, Type type, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> headers,
            OutputStream outputStream) throws IOException, WebApplicationException {
        mapper.writeValue(outputStream, globalPermissionSet);
    }
}
