package eu.ehri.extension;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Converter;
import eu.ehri.project.persistance.EntityBundle;
import eu.ehri.project.views.ActionViews;
import eu.ehri.project.views.IViews;
import eu.ehri.project.views.Query;

import static eu.ehri.extension.RestHelpers.*;

/**
 * Handle CRUD operations on AccessibleEntity's by using the
 * eu.ehri.project.views.Views class generic code. Resources for specific
 * entities can extend this class.
 * 
 * @param <E>
 *            The specific AccesibleEntity derived class
 */
public class EhriNeo4jFramedResource<E extends AccessibleEntity> extends
        AbstractRestResource {

    public static final int DEFAULT_LIST_LIMIT = 20;

    protected final IViews<E> views;
    protected final Query<E> querier;
    protected final Class<E> cls;
    protected final Converter converter = new Converter();

    /**
     * Constructor
     * 
     * @param database
     *            Injected neo4j database
     * @param cls
     *            The 'entity' class
     */
    public EhriNeo4jFramedResource(@Context GraphDatabaseService database,
            Class<E> cls) {
        super(database);
        this.cls = cls;
        views = new ActionViews<E>(graph, cls);
        querier = new Query<E>(graph, cls);

    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     * 
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     * @throws PermissionDenied
     */
    public StreamingOutput list(Integer offset, Integer limit)
            throws ItemNotFound, BadRequester {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonFactory f = new JsonFactory();
        final Iterable<E> list = querier.setOffset(offset).setLimit(limit)
                .list(getRequesterUserProfile());

        // FIXME: I don't understand this streaming output system well
        // enough
        // to determine whether this actually streams or not. It certainly
        // doesn't look like it.
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException,
                    WebApplicationException {
                JsonGenerator g = f.createJsonGenerator(arg0);
                g.writeStartArray();
                for (E item : list) {
                    try {
                        mapper.writeValue(g, converter.vertexFrameToData(item));
                    } catch (SerializationError e) {
                        throw new RuntimeException(e);
                    }
                }
                g.writeEndArray();
                g.close();
            }
        };
    }

    /**
     * Create an instance of the 'entity' in the database
     * 
     * @param json
     *            The json representation of the entity to create (no vertex
     *            'id' fields)
     * @return The response of the create request, the 'location' will contain
     *         the url of the newly created instance.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response create(String json) throws PermissionDenied,
            ValidationError, IntegrityError, DeserializationError,
            ItemNotFound, BadRequester {

        try {
            EntityBundle<VertexFrame> entityBundle = converter
                    .jsonToBundle(json);
            E entity = views.create(converter.bundleToData(entityBundle),
                    getRequesterUserProfile());
            String jsonStr = converter.vertexFrameToJson(entity);
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI docUri = ub.path(entity.asVertex().getId().toString()).build();

            return Response.status(Status.CREATED).location(docUri)
                    .entity((jsonStr).getBytes()).build();

        } catch (SerializationError e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        }
    }

    /**
     * Retieve (get) an instance of the 'entity' in the database
     * 
     * @param id
     *            The vertex id
     * @return The response of the request, which contains the json
     *         representation
     * @throws PermissionDenied
     * @throws BadRequester
     */
    public Response retrieve(long id) throws PermissionDenied, BadRequester {
        try {
            E entity = views.detail(graph.getVertex(id, cls),
                    getRequesterUserProfile());
            String jsonStr = new Converter().vertexFrameToJson(entity);

            return Response.status(Status.OK).entity((jsonStr).getBytes())
                    .build();
        } catch (SerializationError e) {
            // Most likely there was no such item (wrong id)
            // BETTER get a different Exception for that?
            //
            // so we would need to return a BADREQUEST, or NOTFOUND

            return Response.status(Status.NOT_FOUND)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        }
    }

    /**
     * Retieve (get) an instance of the 'entity' in the database
     * 
     * @param id
     *            The Entities identifier string
     * @return The response of the request, which contains the json
     *         representation
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    public Response retrieve(String id) throws PermissionDenied, ItemNotFound,
            BadRequester {
        return retrieve(AccessibleEntity.IDENTIFIER_KEY, id);
    }

    /**
     * Retieve (get) an instance of the 'entity' in the database
     * 
     * @param key
     *            The key to search
     * @param value
     *            The key's value
     * @return The response of the request, which contains the json
     *         representation
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws BadRequester
     */
    public Response retrieve(String key, String value) throws ItemNotFound,
            PermissionDenied, BadRequester {
        try {
            E entity = querier.get(key, value, getRequesterUserProfile());
            String jsonStr = new Converter().vertexFrameToJson(entity);

            return Response.status(Status.OK).entity((jsonStr).getBytes())
                    .build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Update (change) an instance of the 'entity' in the database
     * 
     * @param json
     *            The json
     * @return The response of the update request
     * @throws PermissionDenied
     * @throws IntegrityError
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response update(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {

        try {
            EntityBundle<VertexFrame> entityBundle = converter
                    .jsonToBundle(json);
            E update = views.update(converter.bundleToData(entityBundle),
                    getRequesterUserProfile());
            String jsonStr = new Converter().vertexFrameToJson(update);

            return Response.status(Status.OK).entity((jsonStr).getBytes())
                    .build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Update (change) an instance of the 'entity' in the database
     * 
     * @param id
     *            The items identifier property
     * @param json
     *            The json
     * @return The response of the update request
     * @throws PermissionDenied
     * @throws IntegrityError
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response update(String id, String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        return update(AccessibleEntity.IDENTIFIER_KEY, id, json);
    }

    /**
     * Update (change) an instance of the 'entity' in the database
     * 
     * @param key
     *            The key to search
     * @param value
     *            The key's value
     * @param json
     *            The json
     * @return The response of the update request
     * @throws PermissionDenied
     * @throws IntegrityError
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response update(String key, String value, String json)
            throws PermissionDenied, IntegrityError, ValidationError,
            DeserializationError, ItemNotFound, BadRequester {
        try {
            // FIXME: This is nasty because it searches for an item with the
            // specified key/value and constructs a new bundle containing the
            // item's graph id, which requires an extra
            // serialization/deserialization.
            E entity = querier.get(key, value, getRequesterUserProfile());
            EntityBundle<E> rawBundle = converter.jsonToBundle(json);
            EntityBundle<E> entityBundle = new EntityBundle<E>(entity
                    .asVertex().getId(), rawBundle.getData(), cls,
                    rawBundle.getRelations());
            return update(converter.bundleToJson(entityBundle));
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Delete (remove) an instance of the 'entity' in the database
     * 
     * @param id
     *            The vertex id
     * @return The response of the delete request
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    protected Response delete(Long id) throws PermissionDenied,
            ValidationError, ItemNotFound, BadRequester {
        try {
            views.delete(graph.getVertex(id, cls), getRequesterUserProfile());
            return Response.status(Status.OK).build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Delete (remove) an instance of the 'entity' in the database
     * 
     * @param id
     *            The vertex id
     * @return The response of the delete request
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     * @throws SerializationError
     */
    protected Response delete(String id) throws PermissionDenied, ItemNotFound,
            ValidationError, BadRequester {
        try {
            E entity = querier.get(AccessibleEntity.IDENTIFIER_KEY, id,
                    getRequesterUserProfile());
            views.delete(entity, getRequesterUserProfile());
            return Response.status(Status.OK).build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }
}
