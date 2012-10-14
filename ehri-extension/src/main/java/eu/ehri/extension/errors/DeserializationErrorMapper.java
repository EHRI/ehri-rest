package eu.ehri.extension.errors;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.ObjectMapper;

import eu.ehri.project.exceptions.DeserializationError;

@Provider
public class DeserializationErrorMapper implements ExceptionMapper<DeserializationError> {

	@SuppressWarnings("serial")
    @Override
	public Response toResponse(final DeserializationError e) {
        Map<String, Object> out = new HashMap<String, Object>() {
            {
                put("error", DeserializationError.class.getSimpleName());
                put("details", e.getMessage());
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
