package eu.ehri.extension;

import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

//import org.apache.log4j.Logger;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.views.AclViews;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;

import eu.ehri.extension.errors.BadRequester;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.LoggingCrudViews;
import eu.ehri.project.views.Query;

/**
 * Handle CRUD operations on AccessibleEntity's by using the
 * eu.ehri.project.views.Views class generic code. Resources for specific
 * entities can extend this class.
 *
 * @param <E> The specific AccessibleEntity derived class
 */
public class AbstractAccessibleEntityResource<E extends AccessibleEntity>
        extends AbstractRestResource {

    // FIXME: Logger gives NoClassDefFound when run on server, probably because
    // Log4j isn't available in the default Neo4j server/lib dir.
    // protected final static Logger logger = Logger
    // .getLogger(AbstractAccessibleEntityResource.class);

    protected final LoggingCrudViews<E> views;
    protected final AclViews aclViews;
    protected final Query<E> querier;
    protected final Class<E> cls;

    /**
     * Functor used to post-process items.
     */
    public static interface PostProcess<E extends AccessibleEntity> {
        public void process(E frame) throws PermissionDenied;
    }

    public static class NoOpPostProcess<E extends AccessibleEntity> implements PostProcess<E> {
        @Override
        public void process(E frame) {}
    }

    private final PostProcess<E> noOpPostProcess = new NoOpPostProcess<E>();

    /**
     * Constructor
     *
     * @param database Injected neo4j database
     * @param cls      The 'entity' class
     */
    public AbstractAccessibleEntityResource(
            @Context GraphDatabaseService database, @Context HttpHeaders requestHeaders, Class<E> cls) {
        super(database, requestHeaders);
        this.cls = cls;
        views = new LoggingCrudViews<E>(graph, cls);
        aclViews = new AclViews(graph);
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
        checkNotInTransaction();
        final Query.Page<E> page = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters).page(getRequesterUserProfile());
        return streamingPage(page);
    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     *
     * @return A streaming list
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
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public StreamingOutput list(Integer offset, Integer limit,
            Iterable<String> order, Iterable<String> filters)
            throws ItemNotFound, BadRequester {
        checkNotInTransaction();
        final Query<E> query = querier.setOffset(offset).setLimit(limit)
                .orderBy(order).filter(filters);
        return streamingList(query.list(getRequesterUserProfile()));
    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     *
     * @return List of entities
     * @throws ItemNotFound
     * @throws BadRequester
     */
    public StreamingOutput list(Integer offset, Integer limit)
            throws ItemNotFound, BadRequester {
        checkNotInTransaction();
        return list(offset, limit, Lists.<String>newArrayList(),
                Lists.<String>newArrayList());
    }

    /**
     * Count items accessible to a given user.
     *
     * @param filters A set of query filters
     * @return Number of items.
     * @throws BadRequester
     */
    public Response count(Iterable<String> filters) throws BadRequester {
        Long count = querier.filter(filters).count(getRequesterUserProfile());
        checkNotInTransaction();
        return numberResponse(count);
    }

    /**
     * Create an instance of the 'entity' in the database
     *
     * @param json        The json representation of the entity to create (no vertex
     *                    'id' fields)
     * @param accessorIds List of accessors who can initially view this item
     * @param postProcess A PostProcess functor. This is most commonly used to create
     *                    additional relationships on the created item.
     * @return The response of the create request, the 'location' will contain
     *         the url of the newly created instance.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws IntegrityError
     * @throws DeserializationError
     * @throws BadRequester
     */
    public Response create(String json, List<String> accessorIds, PostProcess<E> postProcess)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, BadRequester {
        checkNotInTransaction();
        Accessor user = getRequesterUserProfile();
        Bundle entityBundle = Bundle.fromString(json);
        try {
            E entity = views.create(entityBundle, user, getLogMessage());
            aclViews.setAccessors(entity, getAccessors(accessorIds, user), user);

            URI docUri = uriInfo.getBaseUriBuilder()
                    .path(getClass())
                    .path(entity.getId()).build();

            postProcess.process(entity);

            graph.getBaseGraph().commit();
            return Response.status(Status.CREATED).location(docUri)
                    .entity(getRepresentation(entity).getBytes()).build();
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }

    public Response create(String json, List<String> accessorIds)
            throws PermissionDenied, ValidationError, IntegrityError,
            DeserializationError, BadRequester {
        return create(json, accessorIds, noOpPostProcess);
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
    public Response retrieve(String id) throws AccessDenied, ItemNotFound,
            BadRequester {
        checkNotInTransaction();
        try {
            E entity = views.detail(id, getRequesterUserProfile());
            return Response.status(Status.OK)
                    .entity(getRepresentation(entity).getBytes()).build();
        } catch (SerializationError e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Update (change) an instance of the 'entity' in the database.
     *
     * @param json The json
     * @return The response of the update request
     * @throws ItemNotFound
     * @throws PermissionDenied
     * @throws IntegrityError
     * @throws ValidationError
     * @throws DeserializationError
     * @throws BadRequester
     */
    public Response update(String json) throws PermissionDenied,
            IntegrityError, ValidationError, DeserializationError,
            BadRequester, ItemNotFound {
        checkNotInTransaction();
        try {
            Bundle entityBundle = Bundle.fromString(json);
            Mutation<E> update = views
                    .update(entityBundle, getRequesterUserProfile(), getLogMessage());
            graph.getBaseGraph().commit();
            return Response.status(Status.OK)
                    .entity(getRepresentation(update.getNode()).getBytes())
                    .build();
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
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
     * @param json The json
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
        try {
            E entity = views.detail(id, getRequesterUserProfile());
            Bundle rawBundle = Bundle.fromString(json);
            if (isPatch()) {
                Serializer depSerializer = new Serializer.Builder(graph).dependentOnly().build();
                Bundle existing = depSerializer.vertexFrameToBundle(entity);
                rawBundle = existing.mergeDataWith(rawBundle);
            }
            Bundle entityBundle = new Bundle(entity.getId(), getEntityType(),
                    rawBundle.getData(), rawBundle.getRelations());
            return update(entityBundle.toJson());
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
    protected Response delete(String id) throws AccessDenied, PermissionDenied, ItemNotFound,
            ValidationError, BadRequester {
        checkNotInTransaction();
        try {
            views.delete(id, getRequesterUserProfile(), getLogMessage());
            graph.getBaseGraph().commit();
            return Response.status(Status.OK).build();
        } catch (SerializationError serializationError) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(serializationError);
        } finally {
            cleanupTransaction();
        }
    }

    // Helpers

    private EntityClass getEntityType() {
        return ClassUtils.getEntityType(cls);
    }

    protected Set<Accessor> getAccessors(List<String> accessorIds,
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
}
