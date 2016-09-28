package eu.ehri.extension.errors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import graphql.GraphQLError;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionError extends WebApplicationException {
    private static final ObjectMapper mapper = new ObjectMapper();

    public ExecutionError(List<GraphQLError> errors) {
        super(Response.status(Response.Status.BAD_REQUEST)
                .entity(errorToJson(errors).getBytes(Charsets.UTF_8)).build());
    }

    private static String errorToJson(List<GraphQLError> errors) {
        try {
            return mapper.writeValueAsString(
                    ImmutableMap.of("errors", errors.stream().map(e -> ImmutableMap.of(
                            "message", e.getMessage(),
                            "location", e.getLocations().stream().map(s -> ImmutableMap.of(
                                    "line", s.getLine(),
                                    "column", s.getColumn()
                            )).collect(Collectors.toList()),
                            "type", e.getErrorType().name()
                            )).collect(Collectors.toList())
                    )
            );
        } catch (JsonProcessingException err) {
            throw new RuntimeException(err);
        }
    }
}
