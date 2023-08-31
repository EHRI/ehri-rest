package eu.ehri.project.importers.json;

import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * Batch operations for JSON data in Bundle format.
 */
public class BatchOperations {
    private static final Logger logger = LoggerFactory.getLogger(BatchOperations.class);
    private static final String STREAM_SOURCE_KEY = "-";
    private final FramedGraph<?> graph;
    private final ActionManager actionManager;
    private final BundleManager dao;
    private final Serializer serializer;
    private final GraphManager manager;
    private final PermissionScope scope;
    private final boolean version;
    private final boolean tolerant;
    private final List<ImportCallback> callbacks;

    /**
     * Constructor.
     *
     * @param graph     the graph object
     * @param scopeOpt  a nullable scope entity
     * @param version   whether to created versioned for changed items
     * @param tolerant  whether to allow individual validation errors
     *                  without failing the entire batch
     * @param callbacks a set of import callbacks to run on item import
     */
    public BatchOperations(FramedGraph<?> graph, PermissionScope scopeOpt, boolean version, boolean tolerant,
                           List<ImportCallback> callbacks) {
        this.graph = graph;
        this.scope = Optional.ofNullable(scopeOpt).orElse(SystemScope.getInstance());
        this.version = version;
        this.tolerant = tolerant;
        this.callbacks = callbacks;

        this.manager = GraphManagerFactory.getInstance(graph);
        this.actionManager = new ActionManager(graph, scope);
        this.dao = new BundleManager(graph, scope.idPath());
        this.serializer = new Serializer.Builder(graph).dependentOnly().build();
    }

    /**
     * Simple constructor, with system scope, version creation
     * activated and tolerant mode off.
     *
     * @param graph the graph object
     */
    public BatchOperations(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance(), true, false, Collections.emptyList());
    }

    /**
     * Toggle tolerant mode, which will prevent the entire batch
     * failing if a single item fails to validate.
     *
     * @param tolerant true or false
     * @return a new batch operation manager
     */
    public BatchOperations setTolerant(boolean tolerant) {
        return new BatchOperations(graph, scope, version, tolerant, callbacks);
    }

    /**
     * Toggle versioning for item updates.
     *
     * @param versioning true or false
     * @return a new batch operation manager
     */
    public BatchOperations setVersioning(boolean versioning) {
        return new BatchOperations(graph, scope, versioning, tolerant, callbacks);
    }

    /**
     * Set the permission scope.
     *
     * @param scope a permission scope frame
     * @return a new batch operation manager
     */
    public BatchOperations setScope(PermissionScope scope) {
        return new BatchOperations(graph, scope, version, tolerant, callbacks);
    }

    /**
     * Add import callbacks to the importer. Note: order of execution
     * is undefined.
     *
     * @param callbacks one or more ImportCallback instances
     * @return a new batch operation manager
     */
    public BatchOperations withCallbacks(ImportCallback... callbacks) {
        List<ImportCallback> newCallbacks = Lists.newArrayList(callbacks);
        newCallbacks.addAll(this.callbacks);
        return new BatchOperations(graph, scope, version, tolerant, newCallbacks);
    }

    /**
     * Create or update a batch of items.
     *
     * @param inputStream an input stream containing a JSON list of
     *                    bundles corresponding to the items to update
     *                    or create.
     * @param actioner    the current user
     * @param logMessage  a log message
     * @return an import log
     * @throws DeserializationError if the input stream is not well-formed
     * @throws ValidationError      if data constraints are not met
     */
    public ImportLog batchImport(InputStream inputStream, Actioner actioner, Optional<String> logMessage)
            throws DeserializationError, ValidationError {
        ActionManager.EventContext ctx = actionManager.newEventContext(actioner,
                EventTypes.modification, logMessage);
        ImportLog log = new ImportLog(logMessage.orElse(null));
        try (CloseableIterable<Bundle> bundleIter = Bundle.bundleStream(inputStream)) {
            for (Bundle bundle : bundleIter) {
                try {
                    Mutation<Accessible> mutation = dao.createOrUpdate(bundle, Accessible.class);
                    String id = mutation.getNode().getId();
                    switch (mutation.getState()) {
                        case UPDATED:
                            log.addUpdated(STREAM_SOURCE_KEY, id);
                            ctx.addSubjects(mutation.getNode());
                            if (version) {
                                mutation.getPrior().ifPresent(b ->
                                        ctx.createVersion(mutation.getNode(), b));
                            }
                            break;
                        case CREATED:
                            log.addCreated(STREAM_SOURCE_KEY, id);
                            ctx.addSubjects(mutation.getNode());
                            break;
                        default:
                            log.addUnchanged(STREAM_SOURCE_KEY, id);
                    }
                    for (ImportCallback callback : callbacks) {
                        callback.itemImported(mutation);
                    }
                } catch (ValidationError e) {
                    if (!tolerant) {
                        throw e;
                    } else {
                        log.addError(bundle.getId(), e.getMessage());
                        logger.warn("Validation error patching {}: {}", bundle.getId(), e);
                    }
                }
            }
            return log.committing(ctx);
        } catch (RuntimeJsonMappingException e) {
            throw new DeserializationError("Error reading JSON stream:", e);
        }
    }

    /**
     * Update a batch of items.
     *
     * @param inputStream an input stream containing a JSON list of
     *                    bundles corresponding to the items to update.
     * @param actioner    the current user
     * @param logMessage  a log message
     * @return an import log
     * @throws ItemNotFound         if one of the items in the input stream does not exist
     * @throws DeserializationError if the input stream is not well-formed
     * @throws ValidationError      if data constraints are not met
     */
    public ImportLog batchUpdate(InputStream inputStream, Actioner actioner, Optional<String> logMessage)
            throws DeserializationError, ItemNotFound, ValidationError {
        ActionManager.EventContext ctx = actionManager.newEventContext(actioner,
                EventTypes.modification, logMessage);
        ImportLog log = new ImportLog(logMessage.orElse(null));
        try (CloseableIterable<Bundle> bundleIter = Bundle.bundleStream(inputStream)) {
            for (Bundle bundle : bundleIter) {
                try {
                    Entity entity = manager.getEntity(bundle.getId(), bundle.getType().getJavaClass());
                    Bundle oldBundle = serializer.entityToBundle(entity);
                    Bundle newBundle = oldBundle.mergeDataWith(bundle);
                    Mutation<Accessible> update = dao.update(newBundle, Accessible.class);
                    switch (update.getState()) {
                        case UPDATED:
                            log.addUpdated(STREAM_SOURCE_KEY, newBundle.getId());
                            ctx.addSubjects(update.getNode());
                            if (version) {
                                ctx.createVersion(entity, oldBundle);
                            }
                            break;
                        case UNCHANGED:
                            log.addUnchanged(STREAM_SOURCE_KEY, newBundle.getId());
                            break;
                        default:
                            throw new RuntimeException("Unexpected status in batch update: " + update.getState());
                    }
                } catch (ValidationError | ItemNotFound e) {
                    if (!tolerant) {
                        throw e;
                    } else {
                        log.addError(bundle.getId(), e.getMessage());
                        logger.warn("Validation error patching {}: {}", bundle.getId(), e);
                    }
                }
            }
            return log.committing(ctx);
        } catch (SerializationError serializationError) {
            throw new RuntimeException(serializationError);
        } catch (RuntimeJsonMappingException e) {
            throw new DeserializationError("Error reading JSON stream:", e);
        }
    }

    /**
     * Delete a batch of items by ID.
     *
     * @param ids        a list of item IDs
     * @param actioner   the current user
     * @param logMessage an optional log message
     * @return the number of items deleted
     * @throws ItemNotFound if one or more of the input IDs does not exist and tolerant
     *                      mode is not enabled
     */
    public int batchDelete(Collection<String> ids, Actioner actioner, Optional<String> logMessage)
            throws ItemNotFound {
        boolean logged = false;
        int done = 0;
        if (!ids.isEmpty()) {
            try {
                ActionManager.EventContext ctx = actionManager.newEventContext(actioner,
                        EventTypes.deletion, logMessage);
                for (String id : ids) {
                    try {
                        Entity entity = manager.getEntity(id, Entity.class);
                        ctx = ctx.addSubjects(entity.as(Accessible.class));
                        if (version) {
                            ctx = ctx.createVersion(entity);
                        }
                        logged = true;
                    } catch (ItemNotFound e) {
                        if (!tolerant) {
                            throw e;
                        }
                    }
                }
                if (logged) {
                    ctx.commit();
                }
                try {
                    for (String id : ids) {
                        dao.delete(serializer.entityToBundle(manager.getEntity(id, Entity.class)));
                        done++;
                    }
                } catch (ItemNotFound e) {
                    if (!tolerant) {
                        throw e;
                    }
                }
            } catch (SerializationError serializationError) {
                throw new RuntimeException(serializationError);
            }
        }
        return done;
    }
}
