package eu.ehri.extension.errors.mappers;

import eu.ehri.project.exceptions.PermissionDenied;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class PermissionDeniedMapper implements ExceptionMapper<PermissionDenied> {

    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("serial")
    @Override
	public Response toResponse(final PermissionDenied e) {
        Map<String, Object> out = new HashMap<String, Object>() {
            {
                put("error", PermissionDenied.class.getSimpleName());
                put("details", new HashMap<String, String>() {
                    {
                        put("message", e.getMessage());
                        put("accessor", e.getAccessor());
                        put("scope", e.getScope());
                        put("item", e.getEntity());
                    }
                });
            }
        };
		try {
            return Response.status(Status.UNAUTHORIZED)
            	.entity(mapper.writeValueAsBytes(out)).build();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
	}
}
