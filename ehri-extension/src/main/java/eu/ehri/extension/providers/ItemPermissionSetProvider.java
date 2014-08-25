package eu.ehri.extension.providers;

import eu.ehri.project.acl.ItemPermissionSet;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.exceptions.DeserializationError;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

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
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class ItemPermissionSetProvider implements MessageBodyReader<ItemPermissionSet> {

    private static JsonFactory factory = new JsonFactory();
    private static ObjectMapper mapper = new ObjectMapper(factory);

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return aClass == ItemPermissionSet.class;
    }

    @Override
    public ItemPermissionSet readFrom(Class<ItemPermissionSet> bundleClass, Type type, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String,
            String> headers, InputStream stream) throws IOException, WebApplicationException {

        try {
            return parseMatrix(stream);
        } catch (DeserializationError deserializationError) {
            throw new WebApplicationException(deserializationError, Response.Status.BAD_REQUEST);
        }
    }

    private ItemPermissionSet parseMatrix(InputStream json) throws DeserializationError {
        try {
            TypeReference<HashSet<PermissionType>> typeRef = new TypeReference<HashSet<PermissionType>>() {
            };
            Set<PermissionType> set = mapper.readValue(json, typeRef);
            return ItemPermissionSet.from(set);
        } catch (JsonMappingException e) {
            throw new DeserializationError(e.getMessage());
        } catch (IOException e) {
            throw new DeserializationError(e.getMessage());
        }
    }
}
