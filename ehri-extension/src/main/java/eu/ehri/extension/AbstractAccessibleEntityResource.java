package eu.ehri.extension;

import static eu.ehri.extension.RestHelpers.produceErrorMessageJson;

import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

//import org.apache.log4j.Logger;
import eu.ehri.project.exceptions.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.impl.Query;

/**
 * Handle CRUD operations on AccessibleEntity's by using the
 * eu.ehri.project.views.Views class generic code. Resources for specific
 * entities can extend this class.
 * 
 * @param <E>
 *            The specific AccessibleEntity derived class
 */
public class AbstractAccessibleEntityResource<E extends AccessibleEntity>
        extends AbstractRestResource {

    // FIXME: Logger gives NoClassDefFound when run on server, probably because
    // Log4j isn't available in the default Neo4j server/lib dir.
    // protected final static Logger logger = Logger
    // .getLogger(AbstractAccessibleEntityResource.class);

    protected final LoggingCrudViews<E> views;
    protected final Query<E> querier;
    protected final Class<E> cls;

    /**
     * Constructor
     * 
     * @param database
     *            Injected neo4j database
     * @param cls
     *            The 'entity' class
     */
    public AbstractAccessibleEntityResource(
            @Context GraphDatabaseService database, Class<E> cls) {
        super(database);
        this.cls = cls;
        views = new LoggingCrudViews<E>(graph, cls);
        querier = new Query<E>(graph, cls);

    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     * 
     * @return List of entities
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public StreamingOutput page(Integer offset, Integer limit,
            Iterable<String> order, Iterable<String> filters)
            throws ItemNotFound, BadRequester {
        final Query.Page<E> page = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).page(getRequesterUserProfile());
        return streamingPage(page);
    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     * 
     * @return
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public StreamingOutput page(Integer offset, Integer limit)
            throws ItemNotFound, BadRequester {
        final Query.Page<E> page = querier.setOffset(offset).setLimit(limit)
                .page(getRequesterUserProfile());

        return streamingPage(page);
    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     * 
     * @return List of entities
     * 
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public StreamingOutput list(Integer offset, Integer limit,
            Iterable<String> order, Iterable<String> filters)
            throws ItemNotFound, BadRequester {
        final Query<E> query = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingList(query.list(getRequesterUserProfile()));
    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     * 
     * @return List of entities
     * 
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public StreamingOutput list(Integer offset, Integer limit)
            throws ItemNotFound, BadRequester {
        return list(offset, limit, Lists.<String> newArrayList(),
                Lists.<String> newArrayList());
    }

    /**
     * Create an instance of the 'entity' in the database
     * 
     * @param json
     *            The json representation of the entity to create (no vertex
     *            'id' fields)
     * @param accessorIds
     *            List of accessors who can initially view this item
     * @return The response of the create request, the 'location' will contain
     *         the url of the newly created instance.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     * @throws BadRequester
     */
    public Response create(String json, List<String> accessorIds)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, BadRequester {

        Transaction tx = graph.getBaseGraph().getRawGraph().beginTx();
        try {
            Accessor user = getRequesterUserProfile();
            Bundle entityBundle = Bundle.fromString(json);
            E entity = views.create(entityBundle, user,
                    getLogMessage(getDefaultCreateMessage(getEntityType())));
            // TODO: Move elsewhere
            new AclManager(graph).setAccessors(entity,
                    getAccessors(accessorIds, user));

            String jsonStr = serializer.vertexFrameToJson(entity);
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI docUri = ub.path(manager.getId(entity)).build();
            tx.success();
            return Response.status(Status.CREATED).location(docUri)
                    .entity((jsonStr).getBytes()).build();
        } catch (SerializationError e) {
            tx.failure();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity((produceErrorMessageJson(e)).getBytes()).build();
        } finally {
            tx.finish();
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
     * @throws AccessDenied
     * @throws BadRequester
     */
    public Response retrieve(String id) throws AccessDenied, ItemNotFound,
            BadRequester {
        try {
            E entity = views.detail(manager.getFrame(id, getEntityType(), cls),
                    getRequesterUserProfile());
            String jsonStr = serializer.vertexFrameToJson(entity);
            return Response.status(Status.OK).entity((jsonStr).getBytes())
                    .build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
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
     * @throws AccessDenied
     * @throws BadRequester
     */
    public Response retrieve(String key, String value) throws ItemNotFound,
            AccessDenied, BadRequester {
        try {
            E entity = querier.get(key, value, getRequesterUserProfile());
            String jsonStr = serializer.vertexFrameToJson(entity);
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
            Bundle entityBundle = Bundle.fromString(json);
            E update = views.update(entityBundle, getRequesterUserProfile(),
                    getLogMessage(getDefaultUpdateMessage(getEntityType(), entityBundle.getId())));
            String jsonStr = serializer.vertexFrameToJson(update);

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
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws IntegrityError
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response update(String id, String json) throws AccessDenied, PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        // FIXME: This is nasty because it searches for an item with the
        // specified key/value and constructs a new bundle containing the
        // item's graph id, which requires an extra
        // serialization/deserialization.
        E entity = views.detail(manager.getFrame(id, getEntityType(), cls),
                getRequesterUserProfile());
        Bundle rawBundle = Bundle.fromString(json);
        Bundle entityBundle = new Bundle(manager.getId(entity),
                getEntityType(), rawBundle.getData(), rawBundle.getRelations());
        return update(entityBundle.toJson());
    }

    /**
     * Delete (remove) an instance of the 'entity' in the database
     * 
     * @param id
     *            The vertex id
     * @return The response of the delete request
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     */
    protected Response delete(String id) throws AccessDenied, PermissionDenied, ItemNotFound,
            ValidationError, BadRequester {
        try {
            E entity = views.detail(manager.getFrame(id, getEntityType(), cls),
                    getRequesterUserProfile());
            views.delete(entity, getRequesterUserProfile(),
                    getLogMessage(getDefaultDeleteMessage(getEntityType(), id)));
            return Response.status(Status.OK).build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    // Helpers

    private EntityClass getEntityType() {
        return ClassUtils.getEntityType(cls);
    }

    protected Iterable<Accessor> getAccessors(List<String> accessorIds,
            Accessor current) {

        Set<Vertex> accessorV = Sets.newHashSet();
        Set<Accessor> accessors = Sets.newHashSet();
        for (String id : accessorIds) {
            try {
                Accessor av = manager.getFrame(id, Accessor.class);
                accessors.add(av);
                accessorV.add(av.asVertex());                
            } catch (ItemNotFound e) {
                // FIXME: Using the logger gives a noclassdef found error
                // logger.error("Invalid accessor given: " + id);
                System.err.println("Invalid accessor given: " + id);
            }
        }
        // The current user should always be among the accessors, so add
        // them unless the list is empty.
        if (!accessors.isEmpty() && !accessorV.contains(current.asVertex())) {
            accessors.add(current);
        }
        return accessors;
    }

    /**
     * Get a default message for an item being created.
     *
     * @return
     */
    protected String getDefaultCreateMessage(EntityClass entityClass) {
        return String.format("%s (%s)",
                LoggingCrudViews.DEFAULT_CREATE_LOG,
                entityClass.getName());
    }

    /**
     * Get a default message for an item being updated.
     *
     * @return
     */
    protected String getDefaultUpdateMessage(EntityClass entityClass, String id) {
        return String.format("%s (%s): '%s'",
                LoggingCrudViews.DEFAULT_UPDATE_LOG,
                entityClass.getName(), id);
    }

    /**
     * Get a default message for an item being deleted.
     *
     * @return
     */
    protected String getDefaultDeleteMessage(EntityClass entityClass, String id) {
        return String.format("%s (%s): '%s'",
                LoggingCrudViews.DEFAULT_DELETE_LOG,
                entityClass.getName(), id);
    }
}
