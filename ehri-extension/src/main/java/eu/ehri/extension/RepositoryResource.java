package eu.ehri.extension;

import java.net.URI;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import eu.ehri.project.exceptions.*;
import org.neo4j.graphdb.GraphDatabaseService;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.Query;

/**
 * Provides a RESTfull interface for the Repository
 */
@Path(Entities.REPOSITORY)
public class RepositoryResource extends AbstractAccessibleEntityResource<Repository> {

    public RepositoryResource(@Context GraphDatabaseService database) {
        super(database, Repository.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getRepository(@PathParam("id") String id) throws ItemNotFound,
            AccessDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public StreamingOutput listRepositories(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return list(offset, limit, order, filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/count")
    public Response countDocumentaryUnits(@QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return count(filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/list")
    public StreamingOutput listRepositoryDocumentaryUnits(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        Repository repository = views.detail(manager.getFrame(id, cls), user);
        Iterable<DocumentaryUnit> units = all
                ? repository.getAllCollections()
                : repository.getCollections();
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class).setLimit(limit).setOffset(offset)
                .orderBy(order)
                .filter(filters);
        return streamingList(query.list(units, user));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public Response countRepositoryDocumentaryUnits(
            @PathParam("id") String id,
            @QueryParam(FILTER_PARAM) List<String> filters,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, AccessDenied {
        Accessor user = getRequesterUserProfile();
        Repository repository = views.detail(manager.getFrame(id, cls), user);
        Iterable<DocumentaryUnit> units = all
                ? repository.getAllCollections()
                : repository.getCollections();
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class)
                .filter(filters);
        return Response.ok((query.count(units, user)).toString().getBytes()).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/page")
    public StreamingOutput pageRepositoryDocumentaryUnits(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, AccessDenied, PermissionDenied {
        Accessor user = getRequesterUserProfile();
        Repository repository = views.detail(manager.getFrame(id, cls), user);
        Iterable<DocumentaryUnit> units = all
                ? repository.getAllCollections()
                : repository.getCollections();
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph,
                DocumentaryUnit.class).setLimit(limit).setOffset(offset)
                .orderBy(order)
                .filter(filters);
        return streamingPage(query.page(units, user));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/page")
    public StreamingOutput pageRepositories(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return page(offset, limit, order, filters);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateRepository(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateRepository(@PathParam("id") String id, String json)
            throws AccessDenied, PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteRepository(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester {
        return delete(id);
    }

    /**
     * Create a documentary unit for this repository.
     * 
     * @param id
     * @param json
     * @return
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.DOCUMENTARY_UNIT)
    public Response createRepositoryDocumentaryUnit(@PathParam("id") String id,
            String json, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        Repository repository = views.detail(manager.getFrame(id, cls), user);
        try {
            DocumentaryUnit doc = createDocumentaryUnit(json, repository);
            new AclManager(graph).setAccessors(doc,
                    getAccessors(accessors, user));
            graph.getBaseGraph().commit();
            return buildResponseFromDocumentaryUnit(doc);
        } catch (SerializationError e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        } finally {
            cleanupTransaction();
        }
    }

    // Helpers

    private Response buildResponseFromDocumentaryUnit(DocumentaryUnit doc)
            throws SerializationError {
        String jsonStr = getSerializer().vertexFrameToJson(doc);
        // FIXME: Hide the details of building this path
        URI docUri = UriBuilder.fromUri(uriInfo.getBaseUri())
                .segment(Entities.DOCUMENTARY_UNIT)
                .segment(doc.getId())
                .build();

        return Response.status(Status.CREATED).location(docUri)
                .entity((jsonStr).getBytes()).build();
    }

    private DocumentaryUnit createDocumentaryUnit(String json, Repository repository)
            throws DeserializationError, PermissionDenied, ValidationError,
            IntegrityError, BadRequester {
        Bundle entityBundle = Bundle.fromString(json);

        DocumentaryUnit doc = new LoggingCrudViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class, repository).create(entityBundle,
                getRequesterUserProfile(), getLogMessage());
        // Add it to this repository's collections
        doc.setRepository(repository);
        return doc;
    }
}
