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
