package eu.ehri.extension.errors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import eu.ehri.project.exceptions.ItemNotFound;

@Provider
public class ItemNotFoundMapper implements ExceptionMapper<ItemNotFound> {

	@Override
	public Response toResponse(ItemNotFound e) {
		return Response.status(Status.NOT_FOUND)
			.entity(e.getValue().getBytes()).build();
	}

}
