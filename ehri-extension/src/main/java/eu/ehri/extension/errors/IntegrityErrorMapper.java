package eu.ehri.extension.errors;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.ObjectMapper;

import eu.ehri.project.exceptions.IntegrityError;

@Provider
public class IntegrityErrorMapper implements ExceptionMapper<IntegrityError> {

	@SuppressWarnings("serial")
    @Override
	public Response toResponse(final IntegrityError e) {
        Map<String, Object> out = new HashMap<String, Object>() {
            {
                put("error", IntegrityError.class.getName());
                put("details", new HashMap<String, Object>() {
                    {
                        put("message", e.getMessage());
                        put("fields", ((IntegrityError) e).getFields());
                    }
                });
            }
        };
        try {
            return Response.status(Status.BAD_REQUEST)
                .entity(new ObjectMapper().writeValueAsString(out).getBytes()).build();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
	}
}
