/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.EventsApi;
import eu.ehri.project.core.Tx;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.*;
import eu.ehri.project.exporters.xml.XmlExporter;
import eu.ehri.project.importers.json.BatchOperations;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.utils.Table;
import org.joda.time.DateTime;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.TransformerException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Handle CRUD operations on Accessible's by using the
 * eu.ehri.project.views.Views class generic code. Resources for specific
 * entities can extend this class.
 *
 * @param <E> the specific Accessible derived class
 */
public class AbstractAccessibleResource<E extends Accessible> extends AbstractResource {

    public final static String ITEM_TYPE_PARAM = "type";
    public final static String ITEM_ID_PARAM = "item";
    public final static String EVENT_TYPE_PARAM = "et";
    public final static String USER_PARAM = "user";
    public final static String FROM_PARAM = "from";
    public final static String TO_PARAM = "to";
    public final static String SHOW_PARAM = "show"; // watched, follows
    public final static String AGGREGATION_PARAM = "aggregation";

    protected final AclManager aclManager;
    protected final ActionManager actionManager;
    protected final Class<E> cls;

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
        aclManager = new AclManager(graph);
        actionManager = new ActionManager(graph);
    }

    /**
     * List all instances of the 'entity' accessible to the given user.
     *
     * @return a list of entities
     */
    public Response listItems() {
        try (final Tx tx = beginTx()) {
            Response response = streamingPage(() -> getQuery().page(cls));
            tx.success();
            return response;
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
     * @param scopedApi    the Api instance to use to create the item. This allows callers
     *                     to override the scope used.
     * @param otherCls     the class of the created item.
     * @param <T>          the generic type of class T
     * @return the response of the create request, the 'location' will contain
     * the url of the newly created instance.
     */
    public <T extends Accessible> Response createItem(
            Bundle entityBundle,
            List<String> accessorIds,
            Handler<T> handler,
            Api scopedApi,
            Class<T> otherCls)
            throws PermissionDenied, ValidationError, DeserializationError {
        Accessor user = getRequesterUserProfile();
        T entity = scopedApi.create(entityBundle, otherCls, getLogMessage());
        if (!accessorIds.isEmpty()) {
            api().acl().setAccessors(entity, getAccessors(accessorIds, user));
        }

        // run post-creation callbacks
        handler.process(entity);
        return creationResponse(entity);
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
     */
    public Response createItem(Bundle entityBundle, List<String> accessorIds, Handler<E> handler)
            throws PermissionDenied, ValidationError, DeserializationError {
        return createItem(entityBundle, accessorIds, handler, api(), cls);
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
     */
    public Response getItem(String id) throws ItemNotFound {
        try (final Tx tx = beginTx()) {
            E entity = fetchAndCheckType(id);
            logger.trace("Fetched item: {}", id);
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
     */
    public Response updateItem(Bundle entityBundle)
            throws PermissionDenied, ValidationError, ItemNotFound, DeserializationError {
        Mutation<E> update = api().update(entityBundle, cls, getLogMessage());
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
     */
    public Response updateItem(String id, Bundle rawBundle)
            throws PermissionDenied, ValidationError,
            DeserializationError, ItemNotFound {
        try {
            E entity = fetchAndCheckType(id);
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
     */
    protected void deleteItem(String id, Handler<E> preProcess)
            throws PermissionDenied, ItemNotFound, ValidationError, HierarchyError {
        try {
            E item = fetchAndCheckType(id);
            preProcess.process(item);
            api().delete(id, getLogMessage());
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        }
    }

    /**
     * Delete (remove) an instance of the 'entity' in the database
     *
     * @param id the item's ID
     */
    protected void deleteItem(String id)
            throws PermissionDenied, ItemNotFound, ValidationError, HierarchyError {
        deleteItem(id, noOpHandler);
    }

    /**
     * Delete an item and all of its child items.
     * @param id the item's ID
     * @param children an iterable of child items
     * @return a table of delete item IDs
     */
    protected Table deleteContents(String id, boolean all)
            throws ItemNotFound, PermissionDenied, ValidationError, HierarchyError {
        fetchAndCheckType(id);
        try {
            List<String> data = api().deleteChildren(id, all, getLogMessage());
            return Table.column(data);
        } catch (SerializationError e) {
           throw new RuntimeException(e);
        }
    }

    // Helpers

    protected <T extends Entity> Response exportItemsAsZip(XmlExporter<T> exporter, Iterable<T> items, String lang) {
        return Response.ok((StreamingOutput) outputStream -> {
            try (final Tx tx = beginTx();
                 ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                for (T item : items) {
                    ZipEntry zipEntry = new ZipEntry(item.getId() + ".xml");
                    zipEntry.setComment("Exported from the EHRI portal at " + (DateTime.now()));
                    zos.putNextEntry(zipEntry);
                    exporter.export(item, zos, lang);
                    zos.closeEntry();
                }
                tx.success();
            } catch (TransformerException e) {
                throw new WebApplicationException(e);
            }
        }).type("application/zip").build();
    }

    /**
     * Get an event query builder object.
     *
     * @return a new event query builder
     */
    protected EventsApi getEventsApi() {
        List<EventTypes> eventTypes = Lists.transform(getStringListQueryParam(EVENT_TYPE_PARAM), EventTypes::valueOf);
        List<EntityClass> entityClasses = Lists.transform(getStringListQueryParam(ITEM_TYPE_PARAM), EntityClass::withName);
        List<EventsApi.ShowType> showTypes = Lists.transform(getStringListQueryParam(SHOW_PARAM), EventsApi.ShowType::valueOf);
        List<String> fromStrings = getStringListQueryParam(FROM_PARAM);
        List<String> toStrings = getStringListQueryParam(TO_PARAM);
        List<String> users = getStringListQueryParam(USER_PARAM);
        List<String> ids = getStringListQueryParam(ITEM_ID_PARAM);
        return api()
                .events()
                .withRange(getIntQueryParam(OFFSET_PARAM, 0),
                        getIntQueryParam(LIMIT_PARAM, DEFAULT_LIST_LIMIT))
                .withEventTypes(eventTypes.toArray(new EventTypes[0]))
                .withEntityClasses(entityClasses.toArray(new EntityClass[0]))
                .from(fromStrings.isEmpty() ? null : fromStrings.get(0))
                .to(toStrings.isEmpty() ? null : toStrings.get(0))
                .withUsers(users.toArray(new String[0]))
                .withIds(ids.toArray(new String[0]))
                .withShowType(showTypes.toArray(new EventsApi.ShowType[0]));
    }

    private E fetchAndCheckType(String id) throws ItemNotFound {
        E entity = api().get(id, cls);
        EntityClass entityClass = manager.getEntityClass(entity);
        if (!entityClass.getJavaClass().equals(cls)) {
            throw new ItemNotFound(entity.getId(), entityClass);
        }
        return entity;
    }

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
