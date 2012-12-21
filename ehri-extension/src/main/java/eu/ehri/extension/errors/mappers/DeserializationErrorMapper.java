package eu.ehri.extension.errors.mappers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.ObjectMapper;

import eu.ehri.project.exceptions.DeserializationError;

@Provider
public class DeserializationErrorMapper implements
        ExceptionMapper<DeserializationError> {

    @SuppressWarnings("unused")
    private String getStacktrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

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
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(new ObjectMapper().writeValueAsString(out)
                            .getBytes()).build();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
    }
}
