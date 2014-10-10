package eu.ehri.extension.errors;

import eu.ehri.project.exceptions.DeserializationError;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;


/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class WebDeserializationError extends WebApplicationException {
    private static final ObjectMapper mapper = new ObjectMapper();

    public WebDeserializationError(DeserializationError e) {
        super(Response.status(Status.BAD_REQUEST)
                .entity(errorToJson(e).getBytes()).build());
    }

    public static String errorToJson(final DeserializationError e) {
        try {
            return mapper.writeValueAsString(new HashMap<String, Object>() {
                {
                    put("error", DeserializationError.class.getSimpleName());
                    put("details", e.getMessage());
                }
            });
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
