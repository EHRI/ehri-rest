package eu.ehri.extension.errors.mappers;

import eu.ehri.project.exceptions.IntegrityError;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class IntegrityErrorMapper implements ExceptionMapper<IntegrityError> {

    private final ObjectMapper mapper = new ObjectMapper();

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
                .entity(mapper.writeValueAsBytes(out)).build();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
	}
}
