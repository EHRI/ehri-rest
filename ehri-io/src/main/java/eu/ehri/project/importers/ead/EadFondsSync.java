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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class EadFondsSync {

    private static final Logger logger = LoggerFactory.getLogger(EadFondsSync.class);

    private final Api api;
    private final PermissionScope scope;
    private final Actioner actioner;
    private final SaxImportManager importManager;

    public EadFondsSync(
            Api api,
            PermissionScope scope,
            Actioner actioner,
            SaxImportManager importManager) {
        this.api = api;
        this.scope = scope;
        this.actioner = actioner;
        this.importManager = importManager;
    }

    public interface IngestOperation {
        ImportLog run(ImportManager manager) throws ArchiveException, InputParseError, ValidationError, IOException;
    }

    public class EadFondsSyncError extends Exception {
        EadFondsSyncError(String message, Throwable underlying) {
            super(message, underlying);
        }
    }

    /**
     * Synchronise an archival scope from EAD files. The actual ingest
     * operation is delegated to the given {@link SaxImportManager} instance.
     * <p>
     * For sync to work correctly
     *
     * @param op         the ingest operation
     * @param logMessage a log message that will be attached to the delete event
     * @return a sync log
     */
    public SyncLog sync(IngestOperation op, Set<String> excludes, String logMessage)
            throws ArchiveException, InputParseError, ValidationError, IOException, EadFondsSyncError {

        // Get a mapping of __id to identifier within the scope,
        // pre-ingest...
        // Pre-sync ALL of the local IDs must be unique
        BiMap<String, String> lookup = HashBiMap.create();
        try {
            getAllChildren(scope)
                    .forEach(unit -> lookup.put(unit.getId(), unit.getIdentifier()));
        } catch (IllegalArgumentException e) {
            throw new EadFondsSyncError("Local identifiers are not unique", e);
        }

        // Remove anything specifically excludes...
        excludes.forEach(lookup::remove);

        logger.debug("ITEMS IN BEFORE MAP: {}", lookup.size());

        // Keep track of new, moved, updated local identifiers.
        BiMap<String, String> newIdentifiers = HashBiMap.create();

        // Run the import!
        ImportManager manager = importManager
                .withCallback(m -> {
                    DocumentaryUnit doc = m.getNode().as(DocumentaryUnit.class);
                    newIdentifiers.put(doc.getIdentifier(), doc.getId());
                });
        ImportLog log = op.run(manager);

        Set<String> toDeleteLocalIds = Sets.newHashSet(lookup.values());
        toDeleteLocalIds.removeAll(newIdentifiers.keySet());
        Set<String> allNewLocalIds = Sets.newHashSet(newIdentifiers.keySet());
        allNewLocalIds.removeAll(lookup.values());
        allNewLocalIds.removeAll(toDeleteLocalIds);

        Set<String> allNewGraphIds = Sets.newHashSet();
        allNewLocalIds.forEach(id -> allNewGraphIds.add(newIdentifiers.get(id)));

        logger.debug("NUMBER TO BE DELETED: {}", toDeleteLocalIds.size());
        logger.debug("NUMBER NEWLY CREATED: {}", allNewLocalIds.size());

        // Find moved items...
        // This gets us a map of old __id to new __id
        Map<String, String> oldToNew = findMovedItems(scope, lookup);
        logger.debug("MOVED ITEMS: {}", oldToNew.size());

        // Delete the old items
        Set<String> toDeleteGraphIds = Sets.newHashSet();
        toDeleteLocalIds.forEach(locId -> toDeleteGraphIds.add(lookup.inverse().get(locId)));

        if (!toDeleteLocalIds.isEmpty()) {
            try {
                ActionManager actionManager = api.actionManager().setScope(scope);
                Api api = this.api.enableLogging(false);
                ActionManager.EventContext ctx = actionManager
                        .newEventContext(actioner, EventTypes.deletion, Optional.ofNullable(logMessage));
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

        logger.debug("Committing import transaction...");

        return new SyncLog(log, toDeleteGraphIds, oldToNew, allNewGraphIds);
    }

    private Map<String, String> findMovedItems(PermissionScope scope, Map<String, String> lookup) {
        Map<String, String> moved = Maps.newHashMap();

        logger.debug("Starting moved item scan...");
        long start = System.nanoTime();
        // NB: This method of finding moved items uses a lot of memory for big
        // repositories, but is dramatically faster than the alternative.
        Multimap<String, String> scan = LinkedHashMultimap.create();
        getAllChildren(scope).forEach(item -> scan.put(item.getIdentifier(), item.getId()));

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

    private Iterable<DocumentaryUnit> getAllChildren(PermissionScope scope) {
        switch (scope.getType()) {
            case Entities.DOCUMENTARY_UNIT:
                return scope.as(DocumentaryUnit.class).getAllChildren();
            case Entities.REPOSITORY:
                return scope.as(Repository.class).getAllDocumentaryUnits();
            default:
                return Collections.emptyList();
        }
    }
}
