package eu.ehri.extension.errors;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.ObjectMapper;

import eu.ehri.project.exceptions.PermissionDenied;

@Provider
public class PermissionDeniedMapper implements ExceptionMapper<PermissionDenied> {

	@SuppressWarnings("serial")
    @Override
	public Response toResponse(final PermissionDenied e) {
        Map<String, Object> out = new HashMap<String, Object>() {
            {
                put("error", PermissionDenied.class.getSimpleName());
                put("details", new HashMap<String, String>() {
                    {
                        put("message", e.getMessage());
                        put("accessor", e
                                .getAccessor().getName());
                    }
                });
            }
        };
		try {
            return Response.status(Status.NOT_FOUND)
            	.entity(new ObjectMapper().writeValueAsString(out).getBytes()).build();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
	}

}
