package eu.ehri.extension;

import com.google.common.collect.Sets;
import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.views.AclViews;
import eu.ehri.project.views.Query;
import eu.ehri.project.views.ViewHelper;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Set;


/**
 * Handle CRUD operations on AccessibleEntity's by using the
 * eu.ehri.project.views.Views class generic code. Resources for specific
 * entities can extend this class.
 *
 * @param <E> The specific AccessibleEntity derived class
 */
public class AbstractAccessibleEntityResource<E extends AccessibleEntity>
        extends AbstractRestResource {

    protected final LoggingCrudViews<E> views;
    protected final AclManager aclManager;
    protected final ActionManager actionManager;
    protected final AclViews aclViews;
    protected final Query<E> querier;
    protected final Class<E> cls;
    protected final ViewHelper helper;


    /**
     * Functor used to post-process items.
     */
    public static interface Handler<E extends AccessibleEntity> {
        public void process(E frame) throws PermissionDenied;
    }

    public static class NoOpHandler<E extends AccessibleEntity> implements Handler<E> {
        @Override
        public void process(E frame) {
        }
    }

    private final Handler<E> noOpHandler = new NoOpHandler<E>();

    /**
     * Constructor
     *
     * @param database Injected neo4j database
     * @param cls      The 'entity' class
     */
    public AbstractAccessibleEntityResource(
            @Context GraphDatabaseService database, Class<E> cls) {
        super(database);
        this.cls = cls;
        views = new LoggingCrudViews<E>(graph, cls);
        aclManager = new AclManager(graph);
        actionManager = new ActionManager(graph);
        aclViews = new AclViews(graph);
        querier = new Query<E>(graph, cls);
        helper = new ViewHelper(graph);
    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     *
     * @return List of entities
     * @throws BadRequester
     */
    public Response listItems() throws BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        return streamingPage(getQuery(cls).page(getRequesterUserProfile()));
    }

    /**
     * Count items accessible to a given user.
     *
     * @return Number of items.
     * @throws BadRequester
     */
    public long countItems() throws BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        return getQuery(cls).count();
    }

    /**
     * Create an instance of the 'entity' in the database
     *
     * @param entityBundle A bundle of item data
     *                     'id' fields)
     * @param accessorIds  List of accessors who can initially view this item
     * @param handler      A callback function that allows additional operations
     *                     to be run on the created object after it is initialised
     *                     but before the response is generated. This is useful for adding
     *                     relationships to the new item.
     * @param views        The view instance to use to create the item. This allows callers
     *                     to override the scope and the class used.
     * @param <T>          The generic type of class T
     * @return The response of the create request, the 'location' will contain
     *         the url of the newly created instance.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws BadRequester
     */
    public <T extends AccessibleEntity> Response createItem(Bundle entityBundle, List<String> accessorIds,
                                                            Handler<T> handler, LoggingCrudViews<T> views)
            throws PermissionDenied, ValidationError,
            DeserializationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor user = getRequesterUserProfile();
            T entity = views
                    .create(entityBundle, user, getLogMessage());
            if (!accessorIds.isEmpty()) {
                aclViews.setAccessors(entity, getAccessors(accessorIds, user), user);
            }

            // run post-creation callbacks
            handler.process(entity);

            graph.getBaseGraph().commit();
            return creationResponse(entity);
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Create an instance of the 'entity' in the database
     *
     * @param entityBundle A bundle of item data
     *                     'id' fields)
     * @param accessorIds  List of accessors who can initially view this item
     * @param handler      A callback function that allows additional operations
     *                     to be run on the created object after it is initialised
     *                     but before the response is generated. This is useful for adding
     *                     relationships to the new item.
     * @return The response of the create request, the 'location' will contain
     *         the url of the newly created instance.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws BadRequester
     */
    public Response createItem(Bundle entityBundle, List<String> accessorIds, Handler<E> handler)
            throws PermissionDenied, ValidationError, DeserializationError, BadRequester {
        return createItem(entityBundle, accessorIds, handler, views);
    }

    public Response createItem(Bundle entityBundle, List<String> accessorIds)
            throws PermissionDenied, ValidationError,
            DeserializationError, BadRequester {
        return createItem(entityBundle, accessorIds, noOpHandler);
    }

    /**
     * Retieve (get) an instance of the 'entity' in the database
     *
     * @param id The Entities identifier string
     * @return The response of the request, which contains the json
     *         representation
     * @throws ItemNotFound
     * @throws AccessDenied
     * @throws BadRequester
     */
    public Response getItem(String id) throws AccessDenied, ItemNotFound,
            BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        E entity = views.detail(id, getRequesterUserProfile());
        if (!manager.getEntityClass(entity).getEntityClass().equals(cls)) {
            throw new ItemNotFound(id);
        }
        return single(entity);
    }

    /**
     * Update (change) an instance of the 'entity' in the database.
     *
     * @param entityBundle The bundle
     * @return The response of the update request
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws BadRequester
     */
    public Response updateItem(Bundle entityBundle) throws PermissionDenied,
            ValidationError, DeserializationError,
            BadRequester, ItemNotFound {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Mutation<E> update = views
                    .update(entityBundle, getRequesterUserProfile(), getLogMessage());
            graph.getBaseGraph().commit();
            return single(update.getNode());
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Update (change) an instance of the 'entity' in the database
     * <p/>
     * If the Patch header is true top-level bundle data will be merged
     * instead of overwritten.
     *
     * @param id   The items identifier property
     * @param rawBundle The bundle
     * @return The response of the update request
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public Response updateItem(String id, Bundle rawBundle) throws AccessDenied, PermissionDenied,
            ValidationError, DeserializationError,
            ItemNotFound, BadRequester {
        try {
            E entity = views.detail(id, getRequesterUserProfile());
            if (isPatch()) {
                Serializer depSerializer = new Serializer.Builder(graph).dependentOnly().build();
                Bundle existing = depSerializer.vertexFrameToBundle(entity);
                return updateItem(existing.mergeDataWith(rawBundle));
            } else {
                return updateItem(rawBundle.withId(entity.getId()));
            }
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Delete (remove) an instance of the 'entity' in the database,
     * running a handler callback beforehand.
     *
     * @param id         The vertex id
     * @param preProcess A handler to run before deleting the item
     * @return The response of the delete request
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     */
    protected Response deleteItem(String id, Handler<E> preProcess) throws AccessDenied, PermissionDenied,
            ItemNotFound,
            ValidationError, BadRequester {
        graph.getBaseGraph().checkNotInTransaction();
        try {
            Accessor user = getRequesterUserProfile();
            preProcess.process(views.detail(id, user));
            views.delete(id, user, getLogMessage());
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }

    /**
     * Delete (remove) an instance of the 'entity' in the database
     *
     * @param id The vertex id
     * @return The response of the delete request
     * @throws AccessDenied
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws BadRequester
     */
    protected Response deleteItem(String id) throws AccessDenied, PermissionDenied,
            ItemNotFound,
            ValidationError, BadRequester {
        return deleteItem(id, noOpHandler);
    }

    // Helpers

    /**
     * Get a set of accessor frames given a list of names.
     *
     * @param accessorIds a list of accessor IDs
     * @param current the current accessor
     * @return a set a accessors
     */
    protected Set<Accessor> getAccessors(List<String> accessorIds, Accessor current) {

        Set<Accessor> accessors = Sets.newHashSet();
        for (String id : accessorIds) {
            try {
                Accessor av = manager.getFrame(id, Accessor.class);
                accessors.add(av);
            } catch (ItemNotFound e) {
                logger.warn("Invalid accessor given: {}", id);
            }
        }
        // The current user should always be among the accessors, so add
        // them unless the list is empty.
        if (!accessors.isEmpty()) {
            accessors.add(current);
        }
        return accessors;
    }
}
