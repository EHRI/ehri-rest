package eu.ehri.extension.errors;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.ObjectMapper;

import eu.ehri.project.exceptions.ValidationError;

@Provider
public class ValidationErrorMapper implements ExceptionMapper<ValidationError> {

	@SuppressWarnings("serial")
    @Override
	public Response toResponse(final ValidationError e) {
        Map<String, Object> out = new HashMap<String, Object>() {
            {
                put("error", ValidationError.class.getName());
                put("details", e.getErrors());
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
