package eu.ehri.project.importers.ead;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import eu.ehri.project.api.Api;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.managers.ImportManager;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import org.apache.commons.compress.archivers.ArchiveException;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class EadSync {

    private static final Logger logger = LoggerFactory.getLogger(EadSync.class);

    private final Api api;
    private final PermissionScope scope;
    private final Actioner actioner;
    private final SaxImportManager importManager;

    public EadSync(
            Api api,
            PermissionScope scope,
            Actioner actioner,
            SaxImportManager importManager) {
        this.api = api;
        this.scope = scope;
        this.actioner = actioner;
        this.importManager = importManager;
    }

    public interface EadIngestOperation {
        ImportLog run(ImportManager manager) throws ArchiveException, InputParseError, ValidationError, IOException;
    }

    public class EadSyncError extends Exception {
        EadSyncError(String message, Throwable underlying) {
            super(message, underlying);
        }

        EadSyncError(String message) {
            super(message);
        }
    }

    /**
     * Synchronise an archival scope from EAD files. The actual ingest
     * operation is delegated to the given {@link SaxImportManager} instance.
     * <p>
     * For sync to work correctly the local (scoped) item identifiers
     * <strong>must</strong> be unique in the scope in which sync is
     * taking place.
     *
     * @param op         the ingest operation
     * @param logMessage a log message that will be attached to the delete event
     * @return a sync log
     * @throws EadSyncError if local identifiers are not unique
     */
    public SyncLog sync(EadIngestOperation op, Set<String> excludes, String logMessage)
            throws ArchiveException, InputParseError, ValidationError, IOException, EadSyncError {

        // Get a mapping of __id to identifier within the scope,
        // pre-ingest...
        // Pre-sync ALL of the local IDs must be unique
        BiMap<String, String> lookup = HashBiMap.create();
        try {
            for (DocumentaryUnit unit: itemsInScope(scope)) {
                lookup.put(unit.getId(), unit.getIdentifier());
            }
        } catch (IllegalArgumentException e) {
            throw new EadSyncError("Local identifiers are not unique", e);
        }

        // Remove anything specifically excluded. This would typically
        // be items in the scope that are not being synced in this operation.
        excludes.forEach(lookup::remove);

        logger.debug("Items in scope prior to sync: {}", lookup.size());

        // Keep track of new, moved, updated local identifiers.
        BiMap<String, String> newIdentifiers = HashBiMap.create();

        // Add a callback to the import manager so we can collect the
        // IDs of new items and run the ingest operation.
        ImportManager manager = importManager
                .withCallback(m -> {
                    DocumentaryUnit doc = m.getNode().as(DocumentaryUnit.class);
                    newIdentifiers.put(doc.getIdentifier(), doc.getId());
                });
        ImportLog log = op.run(manager);

        // Local IDs to be deleted are those in the original set and not in the key set
        Set<String> toDeleteLocalIds = Sets.difference(lookup.values(), newIdentifiers.keySet());

        // All-new items are those in the new set but not the old set, minus those to be deleted.
        Set<String> newLocalIds = Sets.difference(newIdentifiers.keySet(), lookup.values());

        // All-new graph IDs...
        Set<String> allNewGraphIds = Sets.newHashSet(
                Maps.filterKeys(newIdentifiers, newLocalIds::contains).values());

        // Get a set of graph IDs for items to be deleted...
        Set<String> toDeleteGraphIds = Maps.filterValues(lookup, toDeleteLocalIds::contains).keySet();

        // Find moved items...
        // This gets us a map of old graph ID to new graph ID
        Map<String, String> oldToNew = findMovedItems(scope, lookup);

        logger.debug("Deleted items: {}, Created items: {}, Moved items: {}",
                toDeleteGraphIds.size(), allNewGraphIds.size(), oldToNew.size());

        if (!toDeleteLocalIds.isEmpty()) {
            try {
                Api api = this.api.enableLogging(false);
                ActionManager actionManager = api.actionManager().setScope(scope);
                ActionManager.EventContext ctx = actionManager
                        .newEventContext(actioner, EventTypes.deletion, Optional.ofNullable(logMessage));
                logger.debug("Deleting {} items", toDeleteGraphIds.size());
                for (String id : toDeleteGraphIds) {
                    DocumentaryUnit item = api.detail(id, DocumentaryUnit.class);
                    ctx.addSubjects(item);
                    ctx.createVersion(item);
                }
                ctx.commit();
                for (String id : toDeleteGraphIds) {
                    api.delete(id);
                }
            } catch (ItemNotFound | SerializationError | PermissionDenied e) {
                throw new RuntimeException("Unexpected error when deleting item", e);
            }
        }

        return new SyncLog(log, toDeleteGraphIds, oldToNew, allNewGraphIds);
    }

    private Map<String, String> findMovedItems(PermissionScope scope, Map<String, String> lookup)
            throws EadSyncError {
        Map<String, String> moved = Maps.newHashMap();

        logger.debug("Starting moved item scan...");
        long start = System.nanoTime();
        // NB: This method of finding moved items uses a lot of memory for big
        // repositories, but is dramatically faster than the alternative.
        Multimap<String, String> scan = LinkedHashMultimap.create();
        itemsInScope(scope).forEach(item -> scan.put(item.getIdentifier(), item.getId()));

        scan.asMap().forEach((localId, graphIds) -> {
            List<String> ids = Lists.newArrayList(graphIds);
            if (ids.size() > 1) {
                Preconditions.checkState(ids.size() == 2,
                        "Unexpected situation in EAD sync. Item " + localId +
                                " cannot be unique since after sync ingest there are it exists in more than two places: " + ids);
                String first = ids.get(0);
                String second = ids.get(1);
                if (lookup.containsKey(first)) {
                    moved.put(first, second);
                } else if (lookup.containsKey(second)) {
                    moved.put(second, first);
                } else {
                    throw new RuntimeException(
                            "Unexpected situation: 'moved' item not found in before-set... " + localId);
                }
            }
        });
        long end = System.nanoTime();
        logger.debug("Completed moved item scan in {} milli secs", (end - start) / 1_000_000);

        return moved;
    }

    private Iterable<DocumentaryUnit> itemsInScope(PermissionScope scope) throws EadSyncError {
        switch (scope.getType()) {
            case Entities.DOCUMENTARY_UNIT:
                // If the scope is a doc unit, the full set of items includes itself
                return Iterables.append(scope.as(DocumentaryUnit.class),
                        scope.as(DocumentaryUnit.class).getAllChildren());
            case Entities.REPOSITORY:
                return scope.as(Repository.class).getAllDocumentaryUnits();
            default:
                throw new EadSyncError("Scope must be a repository or a documentary unit");
        }
    }
}
