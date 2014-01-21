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
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.Query;

/**
 * Provides a RESTfull interface for the DocumentaryUnit
 */
@Path(Entities.DOCUMENTARY_UNIT)
public class DocumentaryUnitResource extends
        AbstractAccessibleEntityResource<DocumentaryUnit> {

    public DocumentaryUnitResource(@Context GraphDatabaseService database) {
        super(database, DocumentaryUnit.class);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response getDocumentaryUnitJson(@PathParam("id") String id)
            throws ItemNotFound, AccessDenied, BadRequester {
        return retrieve(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/list")
    public StreamingOutput listDocumentaryUnits(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return list(offset, limit, order, filters);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/page")
    public StreamingOutput pageDocumentaryUnits(
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters)
            throws ItemNotFound, BadRequester {
        return page(offset, limit, order, filters);
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
    public StreamingOutput listChildDocumentaryUnits(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied {
        DocumentaryUnit parent = manager.getFrame(id, DocumentaryUnit.class);
        Iterable<DocumentaryUnit> units = all
                ? parent.getAllChildren()
                : parent.getChildren();
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph, cls)
                .setOffset(offset).setLimit(limit).filter(filters)
                .orderBy(order).filter(filters);
        return streamingList(query.list(units, getRequesterUserProfile()));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/page")
    public StreamingOutput pageChildDocumentaryUnits(
            @PathParam("id") String id,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit,
            @QueryParam(SORT_PARAM) List<String> order,
            @QueryParam(FILTER_PARAM) List<String> filters,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied {
        DocumentaryUnit parent = manager.getFrame(id, DocumentaryUnit.class);
        Iterable<DocumentaryUnit> units = all
                ? parent.getAllChildren()
                : parent.getChildren();
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph, cls)
                .setOffset(offset).setLimit(limit).filter(filters)
                .orderBy(order).filter(filters);
        return streamingPage(query.page(units, getRequesterUserProfile()));
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/count")
    public Response countChildDocumentaryUnits(
            @PathParam("id") String id,
            @QueryParam(FILTER_PARAM) List<String> filters,
            @QueryParam(ALL_PARAM) @DefaultValue("false") boolean all)
            throws ItemNotFound, BadRequester, PermissionDenied {
        DocumentaryUnit parent = manager.getFrame(id, DocumentaryUnit.class);
        Iterable<DocumentaryUnit> units = all
                ? parent.getAllChildren()
                : parent.getChildren();
        Query<DocumentaryUnit> query = new Query<DocumentaryUnit>(graph, cls)
                .filter(filters);
        return Response.ok((query.count(units,
                getRequesterUserProfile())).toString().getBytes()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    public Response updateDocumentaryUnit(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(json);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}")
    public Response updateDocumentaryUnit(@PathParam("id") String id,
            String json) throws AccessDenied, PermissionDenied, IntegrityError,
            ValidationError, DeserializationError, ItemNotFound, BadRequester {
        return update(id, json);
    }

    @DELETE
    @Path("/{id:.+}")
    public Response deleteDocumentaryUnit(@PathParam("id") String id)
            throws AccessDenied, PermissionDenied, ItemNotFound, ValidationError,
            BadRequester, SerializationError {
        return delete(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
    @Path("/{id:.+}/" + Entities.DOCUMENTARY_UNIT)
    public Response createChildDocumentaryUnit(@PathParam("id") String id,
            String json, @QueryParam(ACCESSOR_PARAM) List<String> accessors)
            throws AccessDenied, PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, ItemNotFound, BadRequester {
        Accessor user = getRequesterUserProfile();
        DocumentaryUnit parent = views.detail(manager.getFrame(id, cls), user);
        try {
            DocumentaryUnit doc = createDocumentaryUnit(json, parent);
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

        try {
            // FIXME: Hide the details of building this path
            URI docUri = UriBuilder.fromUri(uriInfo.getBaseUri())
                    .segment(Entities.DOCUMENTARY_UNIT)
                    .segment(doc.getId()).build();
            return Response.status(Status.CREATED).location(docUri)
                    .entity((jsonStr).getBytes()).build();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
    }

    private DocumentaryUnit createDocumentaryUnit(String json,
            DocumentaryUnit parent) throws DeserializationError,
            PermissionDenied, ValidationError, IntegrityError, BadRequester {
        Bundle entityBundle = Bundle.fromString(json);

        DocumentaryUnit doc = new LoggingCrudViews<DocumentaryUnit>(graph,
                DocumentaryUnit.class, parent).create(entityBundle,
                getRequesterUserProfile(), getLogMessage());
        // NB: We no longer add this item to the
        // parent's Repository directly.
        parent.addChild(doc);
        return doc;
    }
}
