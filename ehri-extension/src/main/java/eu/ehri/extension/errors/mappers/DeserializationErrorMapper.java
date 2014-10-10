package eu.ehri.extension.errors.mappers;

import eu.ehri.extension.errors.WebDeserializationError;
import eu.ehri.project.exceptions.DeserializationError;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;

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
        return Response
                .status(Status.BAD_REQUEST)
                .entity(WebDeserializationError.errorToJson(e)
                        .getBytes()).build();
    }
}
