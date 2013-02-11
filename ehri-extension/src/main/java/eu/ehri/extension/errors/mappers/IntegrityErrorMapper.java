package eu.ehri.extension.errors.mappers;

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
                put("error", IntegrityError.class.getSimpleName());
                put("value", e.getIdValue());
            }
        };
        try {
            return Response.status(Status.BAD_REQUEST)
                .entity(new ObjectMapper().writeValueAsBytes(out)).build();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
	}
}
