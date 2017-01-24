package eu.ehri.project.importers.json;

import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
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
import java.util.List;
import java.util.Optional;


/**
 * Batch operations for JSON data in Bundle format.
 */
public class BatchOperations {
    private static final Logger logger = LoggerFactory.getLogger(BatchOperations.class);
    private final FramedGraph<?> graph;
    private final ActionManager actionManager;
    private final BundleManager dao;
    private final Serializer serializer;
    private final GraphManager manager;
    private final PermissionScope scope;
    private final boolean version;
    private final boolean tolerant;

    /**
     * Constructor.
     *
     * @param graph    the graph object
     * @param scopeOpt a nullable scope entity
     * @param version  whether to created versioned for changed items
     * @param tolerant whether to allow individual validation errors
     *                 without failing the entire batch
     */
    public BatchOperations(FramedGraph<?> graph, PermissionScope scopeOpt, boolean version, boolean tolerant) {
        this.graph = graph;
        this.scope = Optional.ofNullable(scopeOpt).orElse(SystemScope.getInstance());
        this.version = version;
        this.tolerant = tolerant;

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
        this(graph, SystemScope.getInstance(), true, false);
    }

    /**
     * Toggle tolerant mode, which will prevent the entire batch
     * failing if a single item fails to validate.
     *
     * @param tolerant true or false
     * @return a new batch operation manager
     */
    public BatchOperations setTolerant(boolean tolerant) {
        return new BatchOperations(graph, scope, version, tolerant);
    }

    /**
     * Toggle versioning for item updates.
     *
     * @param versioning true or false
     * @return a new batch operation manager
     */
    public BatchOperations setVersioning(boolean versioning) {
        return new BatchOperations(graph, scope, versioning, tolerant);
    }

    /**
     * Set the permission scope.
     *
     * @param scope a permission scope frame
     * @return a new batch operation manager
     */
    public BatchOperations setScope(PermissionScope scope) {
        return new BatchOperations(graph, scope, version, tolerant);
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
     */
    public ImportLog batchImport(InputStream inputStream, Actioner actioner, Optional<String> logMessage)
            throws DeserializationError, ItemNotFound, ValidationError {
        ActionManager.EventContext ctx = actionManager.newEventContext(actioner,
                EventTypes.modification, logMessage);
        ImportLog log = new ImportLog(logMessage.orElse(null));
        try (CloseableIterable<Bundle> bundleIter = Bundle.bundleStream(inputStream)) {
            for (Bundle bundle : bundleIter) {
                try {
                    Mutation<Accessible> update = dao.createOrUpdate(bundle, Accessible.class);
                    switch (update.getState()) {
                        case UPDATED:
                            log.addUpdated();
                            ctx.addSubjects(update.getNode());
                            if (version) {
                                ctx.createVersion(update.getNode(), update.getPrior().get());
                            }
                            break;
                        case CREATED:
                            log.addCreated();
                            ctx.addSubjects(update.getNode());
                        default:
                            log.addUnchanged();
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
            if (log.hasDoneWork()) {
                ctx.commit();
            }
            return log;
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
     */
    public ImportLog batchUpdate(InputStream inputStream, Actioner actioner, Optional<String> logMessage)
            throws DeserializationError, ItemNotFound, ValidationError {
        ActionManager.EventContext ctx = actionManager.newEventContext(actioner,
                EventTypes.modification, logMessage);
        ImportLog log = new ImportLog(logMessage.orElse(null));
        try (CloseableIterable<Bundle> bundleIter = Bundle.bundleStream(inputStream)) {
            for (Bundle bundle : bundleIter) {
                Entity entity = manager.getEntity(bundle.getId(), bundle.getType().getJavaClass());
                Bundle oldBundle = serializer.entityToBundle(entity);
                Bundle newBundle = oldBundle.mergeDataWith(bundle);
                try {
                    Mutation<Accessible> update = dao.update(newBundle, Accessible.class);
                    switch (update.getState()) {
                        case UPDATED:
                            log.addUpdated();
                            ctx.addSubjects(update.getNode());
                            if (version) {
                                ctx.createVersion(entity, oldBundle);
                            }
                            break;
                        case UNCHANGED:
                            log.addUnchanged();
                            break;
                        default:
                            throw new RuntimeException("Unexpected status in batch update: " + update.getState());
                    }
                } catch (ValidationError e) {
                    if (!tolerant) {
                        throw e;
                    } else {
                        log.addError(entity.getId(), e.getMessage());
                        logger.warn("Validation error patching {}: {}", entity.getId(), e);
                    }
                }
            }
            if (log.hasDoneWork()) {
                ctx.commit();
            }
            return log;
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
     */
    public int batchDelete(List<String> ids, Actioner actioner, Optional<String> logMessage)
            throws ItemNotFound {
        if (!ids.isEmpty()) {
            int done = 0;
            try {
                ActionManager.EventContext ctx = actionManager.newEventContext(actioner,
                        EventTypes.deletion, logMessage);
                for (String id : ids) {
                    Entity entity = manager.getEntity(id, Entity.class);
                    ctx = ctx.addSubjects(entity.as(Accessible.class));
                    if (version) {
                        ctx = ctx.createVersion(entity);
                    }
                }
                ctx.commit();
                for (String id : ids) {
                    dao.delete(serializer.entityToBundle(manager.getEntity(id, Entity.class)));
                    done++;
                }
                return done;
            } catch (SerializationError serializationError) {
                throw new RuntimeException(serializationError);
            }
        } else {
            return 0;
        }
    }
}
