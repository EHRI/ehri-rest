/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.extension.base;

import com.google.common.collect.Sets;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.core.Tx;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.models.base.Accessible;
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
 * Handle CRUD operations on Accessible's by using the
 * eu.ehri.project.views.Views class generic code. Resources for specific
 * entities can extend this class.
 *
 * @param <E> the specific Accessible derived class

 */
public class AbstractAccessibleResource<E extends Accessible> extends AbstractRestResource {

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
    public interface Handler<E extends Accessible> {
        void process(E frame) throws PermissionDenied;
    }

    /**
     * Implementation of a Handler that does nothing.
     *
     * @param <E> the specific Accessible derived class
     */
    public static class NoOpHandler<E extends Accessible> implements Handler<E> {
        @Override
        public void process(E frame) {
        }
    }

    private final Handler<E> noOpHandler = new NoOpHandler<>();

    /**
     * Constructor
     *
     * @param database the injected neo4j database
     * @param cls      the entity Java class
     */
    public AbstractAccessibleResource(
            @Context GraphDatabaseService database, Class<E> cls) {
        super(database);
        this.cls = cls;
        views = new LoggingCrudViews<>(graph, cls);
        aclManager = new AclManager(graph);
        actionManager = new ActionManager(graph);
        aclViews = new AclViews(graph);
        querier = new Query<>(graph, cls);
        helper = new ViewHelper(graph);
    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     *
     * @return a list of entities
     */
    public Response listItems() {
        Tx tx = graph.getBaseGraph().beginTx();
        try {
            return streamingPage(getQuery(cls).page(getRequesterUserProfile()), tx);
        } catch (Exception e) {
            tx.close();
            throw e;
        }
    }

    /**
     * Create an instance of the 'entity' in the database
     *
     * @param entityBundle a bundle of item data
     *                     'id' fields)
     * @param accessorIds  a list of accessors who can initially view this item
     * @param handler      a callback function that allows additional operations
     *                     to be run on the created object after it is initialised
     *                     but before the response is generated. This is useful for adding
     *                     relationships to the new item.
     * @param views        the view instance to use to create the item. This allows callers
     *                     to override the scope and the class used.
     * @param <T>          the generic type of class T
     * @return the response of the create request, the 'location' will contain
     * the url of the newly created instance.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     */
    public <T extends Accessible> Response createItem(Bundle entityBundle, List<String> accessorIds,
                                                            Handler<T> handler, LoggingCrudViews<T> views)
            throws PermissionDenied, ValidationError, DeserializationError {
        try {
            Accessor user = getRequesterUserProfile();
            T entity = views
                    .create(entityBundle, user, getLogMessage());
            if (!accessorIds.isEmpty()) {
                aclViews.setAccessors(entity, getAccessors(accessorIds, user), user);
            }

            // run post-creation callbacks
            handler.process(entity);

            return creationResponse(entity);
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    /**
     * Create an instance of the 'entity' in the database
     *
     * @param entityBundle a bundle of item data
     *                     'id' fields)
     * @param accessorIds  a list of accessors who can initially view this item
     * @param handler      a callback function that allows additional operations
     *                     to be run on the created object after it is initialised
     *                     but before the response is generated. This is useful for adding
     *                     relationships to the new item.
     * @return the response of the create request, the 'location' will contain
     * the url of the newly created instance.
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     */
    public Response createItem(Bundle entityBundle, List<String> accessorIds, Handler<E> handler)
            throws PermissionDenied, ValidationError, DeserializationError {
        return createItem(entityBundle, accessorIds, handler, views);
    }

    public Response createItem(Bundle entityBundle, List<String> accessorIds)
            throws PermissionDenied, ValidationError, DeserializationError {
        return createItem(entityBundle, accessorIds, noOpHandler);
    }

    /**
     * Retieve (get) an instance of the 'entity' in the database
     *
     * @param id the Entities identifier string
     * @return the response of the request, which contains the json
     * representation
     * @throws ItemNotFound
     */
    public Response getItem(String id) throws ItemNotFound {
        try (final Tx tx = graph.getBaseGraph().beginTx()) {
            E entity = views.detail(id, getRequesterUserProfile());
            if (!manager.getEntityClass(entity).getJavaClass().equals(cls)) {
                throw new ItemNotFound(id);
            }
            Response response = single(entity);
            tx.success();
            return response;
        }
    }

    /**
     * Update (change) an instance of the 'entity' in the database.
     *
     * @param entityBundle the bundle
     * @return the response of the update request
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     */
    public Response updateItem(Bundle entityBundle)
            throws PermissionDenied, ValidationError, DeserializationError {
        Mutation<E> update = views
                .update(entityBundle, getRequesterUserProfile(), getLogMessage());
        return single(update.getNode());
    }

    /**
     * Update (change) an instance of the 'entity' in the database
     * <p>
     * If the Patch header is true top-level bundle data will be merged
     * instead of overwritten.
     *
     * @param id        the item's ID
     * @param rawBundle the bundle
     * @return the response of the update request
     * @throws PermissionDenied
     * @throws ValidationError
     * @throws DeserializationError
     * @throws ItemNotFound
     */
    public Response updateItem(String id, Bundle rawBundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try {
            E entity = views.detail(id, getRequesterUserProfile());
            if (isPatch()) {
                Serializer depSerializer = new Serializer.Builder(graph).dependentOnly().build();
                Bundle existing = depSerializer.entityToBundle(entity);
                return updateItem(existing.mergeDataWith(rawBundle));
            } else {
                return updateItem(rawBundle.withId(entity.getId()));
            }
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    /**
     * Delete (remove) an instance of the 'entity' in the database,
     * running a handler callback beforehand.
     *
     * @param id         the item's ID
     * @param preProcess a handler to run before deleting the item
     * @return the response of the delete request
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     */
    protected Response deleteItem(String id, Handler<E> preProcess)
            throws PermissionDenied, ItemNotFound, ValidationError {
        try {
            Accessor user = getRequesterUserProfile();
            preProcess.process(views.detail(id, user));
            views.delete(id, user, getLogMessage());
            return Response.status(Status.OK).build();
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    /**
     * Delete (remove) an instance of the 'entity' in the database
     *
     * @param id the item's ID
     * @return the response of the delete request
     * @throws PermissionDenied
     * @throws ItemNotFound
     * @throws ValidationError
     */
    protected Response deleteItem(String id)
            throws PermissionDenied, ItemNotFound, ValidationError {
        return deleteItem(id, noOpHandler);
    }

    // Helpers

    /**
     * Get a set of accessor frames given a list of names.
     *
     * @param accessorIds a list of accessor IDs
     * @param current     the current accessor
     * @return a set a accessors
     */
    protected Set<Accessor> getAccessors(List<String> accessorIds, Accessor current) {

        Set<Accessor> accessors = Sets.newHashSet();
        for (String id : accessorIds) {
            try {
                Accessor av = manager.getEntity(id, Accessor.class);
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
