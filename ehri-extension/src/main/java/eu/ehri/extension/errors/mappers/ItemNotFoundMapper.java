package eu.ehri.extension.errors.mappers;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.ObjectMapper;

import eu.ehri.project.exceptions.ItemNotFound;

@Provider
public class ItemNotFoundMapper implements ExceptionMapper<ItemNotFound> {

    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("serial")
    @Override
	public Response toResponse(final ItemNotFound e) {
        Map<String, Object> out = new HashMap<String, Object>() {
            {
                put("error", ItemNotFound.class.getSimpleName());
                put("details", new HashMap<String, Object>() {
                    {
                        put("message", e.getMessage());
                        put("key", e.getKey());
                        put("value", e.getValue());
                    }
                });
            }
        };
        try {
            return Response.status(Status.NOT_FOUND)
                .entity(mapper.writeValueAsString(out).getBytes()).build();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
	}

}
