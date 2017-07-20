package eu.ehri.project.importers.ead;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.api.Api;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.managers.ImportManager;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Annotatable;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Serializer;
import org.apache.commons.compress.archivers.ArchiveException;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;


/**
 * Synchronise items in a repository or fonds from EAD data.
 */
public class EadSync {

    private static final Logger logger = LoggerFactory.getLogger(EadSync.class);

    private final FramedGraph<?> graph;
    private final Api api;
    private final PermissionScope scope;
    private final Actioner actioner;
    private final SaxImportManager importManager;
    private final GraphManager manager;
    private final Serializer depSerializer;

    public EadSync(
            FramedGraph<?> graph,
            Api api,
            PermissionScope scope,
            Actioner actioner,
            SaxImportManager importManager) {
        this.graph = graph;
        this.api = api;
        this.scope = scope;
        this.actioner = actioner;
        this.importManager = importManager;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.depSerializer = api.serializer().withDependentOnly(true);
    }

    /**
     * An operation that performs the actual ingest, given the {@link ImportManager}
     * and returning an {@link ImportLog}. This is a function because the actual
     * ingest may depend on situational details of the data of no interest to us
     * here.
     */
    public interface EadIngestOperation {
        ImportLog runIngest(ImportManager manager) throws ArchiveException, InputParseError, ValidationError, IOException;
    }

    /**
     * Signal that something has gone wrong with the sync operation.
     */
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
     * @return a {@link SyncLog} instance
     * @throws EadSyncError if local identifiers are not unique
     */
    public SyncLog sync(EadIngestOperation op, Set<String> excludes, String logMessage)
            throws ArchiveException, InputParseError, ValidationError, IOException, EadSyncError {

        // Get a mapping of graph ID to local ID within the scope,
        // Pre-sync, ALL of the local IDs must be unique (and
        // the BiMap will error if they aren't.)
        BiMap<String, String> oldGraphToLocal = HashBiMap.create();
        try {
            for (DocumentaryUnit unit : itemsInScope(scope)) {
                oldGraphToLocal.put(unit.getId(), unit.getIdentifier());
            }
        } catch (IllegalArgumentException e) {
            throw new EadSyncError("Local identifiers are not unique", e);
        }

        // Remove anything specifically excluded. This would typically
        // be items in the scope that are not being synced in this operation.
        excludes.forEach(oldGraphToLocal::remove);

        logger.debug("Items in scope prior to sync: {}", oldGraphToLocal.size());

        // Keep a mapping of new graph to local IDs in the ingest operation
        BiMap<String, String> newGraphToLocal = HashBiMap.create();

        // Add a callback to the import manager so we can collect the
        // IDs of new items and run the ingest operation.
        ImportManager manager = importManager.withCallback(m -> {
            DocumentaryUnit doc = m.getNode().as(DocumentaryUnit.class);
            newGraphToLocal.put(doc.getId(), doc.getIdentifier());
        });
        // Actually run the ingest...
        ImportLog log = op.runIngest(manager);

        // Find moved items... this gets us a map of old graph ID to new graph ID
        BiMap<String, String> movedGraphIds = findMovedItems(scope, oldGraphToLocal);

        // All-new items are those in the new set but not the old set.
        Set<String> allNewGraphIds = Sets.difference(newGraphToLocal.keySet(), oldGraphToLocal.keySet());

        // Items to be deleted are in the old set but not in the new set.
        Set<String> allDeletedGraphIds = Sets.difference(oldGraphToLocal.keySet(), newGraphToLocal.keySet());

        // These are just the created or deleted items, minus those that have moved.
        Set<String> createdIds = Sets.difference(allNewGraphIds, movedGraphIds.values());
        Set<String> deletedIds = Sets.difference(allDeletedGraphIds, movedGraphIds.keySet());

        logger.debug("Created items: {}, Deleted items: {}, Moved items: {}",
                createdIds.size(), deletedIds.size(), movedGraphIds.size());

        // Transfer user-generated annotations and links between moved items...
        transferUserGeneratedContent(movedGraphIds, logMessage);

        // Delete items that have been deleted or moved...
        deleteDeadOrMoved(allDeletedGraphIds, logMessage);

        return new SyncLog(log, createdIds, deletedIds, movedGraphIds);
    }

    private void transferUserGeneratedContent(BiMap<String, String> movedGraphIds, String logMessage) {
        if (!movedGraphIds.isEmpty()) {
            try {
                int modified = 0;
                Api api = this.api.enableLogging(false);
                ActionManager actionManager = api.actionManager().setScope(scope);
                ActionManager.EventContext ctx = actionManager
                        .newEventContext(actioner, EventTypes.modification, Optional.ofNullable(logMessage));

                for (Map.Entry<String, String> entry : movedGraphIds.entrySet()) {
                    DocumentaryUnit from = api.detail(entry.getKey(), DocumentaryUnit.class);
                    DocumentaryUnit to = api.detail(entry.getValue(), DocumentaryUnit.class);
                    boolean changed = transferUserGeneratedContent(from, to);
                    if (changed) {
                        ctx.addSubjects(to);
                        modified++;
                    }
                }

                if (modified > 0) {
                    ctx.commit();
                }

                logger.debug("Transferred user-generated content from {} items...", modified);
            } catch (SerializationError | ItemNotFound e) {
                throw new RuntimeException("Unexpected error when transferring user generated content", e);
            }
        }
    }

    private boolean transferUserGeneratedContent(DocumentaryUnit from, DocumentaryUnit to)
            throws SerializationError, ItemNotFound {
        int moved = 0;
        List<Link> links = Lists.newArrayList(from.getLinks());
        for (Link link : links) {
            if (link.getLinkBodies().iterator().hasNext()) {
                // Skip links with a body...
                continue;
            }
            logger.debug("Moving link from {} to {}...", from.getId(), to.getId());
            to.addLink(link);
            moved++;
        }
        List<Annotation> annotations = Lists.newArrayList(from.getAnnotations());
        for (Annotation annotation : annotations) {
            logger.debug("Moving annotation from {} to {}...", from.getId(), to.getId());
            to.addAnnotation(annotation);
            for (Annotatable part : annotation.getTargetParts()) {
                findPart(part, to).ifPresent(altPart -> {
                    logger.debug("Found equivalent target part: {}", altPart.getId());
                    altPart.addAnnotationPart(annotation);
                });
            }
            moved++;
        }
        return moved > 0;
    }

    private Optional<Annotatable> findPart(Annotatable orig, DocumentaryUnit newParent)
            throws SerializationError, ItemNotFound {
        Bundle newParentBundle = depSerializer.entityToBundle(newParent);
        Bundle dep = depSerializer.entityToBundle(orig);

        BiFunction<Bundle, Bundle, Boolean> isEquivalentDescription =
                (Bundle a, Bundle b) -> Objects.equals(a.getType(), b.getType())
                        && Objects.equals(a.getDataValue(Ontology.LANGUAGE), b.getDataValue(Ontology.LANGUAGE))
                        && Objects.equals(a.getDataValue(Ontology.IDENTIFIER_KEY), b.getDataValue(Ontology.IDENTIFIER_KEY));

        Optional<Bundle> bundle = newParentBundle.find(b ->
                b.equals(dep) || isEquivalentDescription.apply(dep, b));
        return bundle.isPresent()
                ? Optional.of(manager.getEntity(bundle.get().getId(), Annotatable.class))
                : Optional.empty();
    }

    private void deleteDeadOrMoved(Set<String> toDeleteGraphIds, String logMessage) throws ValidationError {
        if (!toDeleteGraphIds.isEmpty()) {
            try {
                Api api = this.api.enableLogging(false);
                ActionManager actionManager = api.actionManager().setScope(scope);
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
                logger.debug("Finished deleting {} items...", toDeleteGraphIds.size());
            } catch (ItemNotFound | SerializationError | PermissionDenied e) {
                throw new RuntimeException("Unexpected error when deleting item", e);
            }
        }
    }

    private BiMap<String, String> findMovedItems(PermissionScope scope, Map<String, String> lookup)
            throws EadSyncError {
        BiMap<String, String> moved = HashBiMap.create();

        logger.debug("Starting moved item scan...");
        long start = System.nanoTime();
        // NB: This method of finding moved items uses a lot of memory for big
        // repositories, but is dramatically faster than the alternative, which
        // is for every item to loop over every _other_ item and check if it has the
        // same local ID and a different graph one. This becomes impractically slow
        // with many thousands of items.
        // Here we use a multimap and look for items where one local ID maps to
        // two graph IDs.
        Multimap<String, String> localToGraph = LinkedHashMultimap.create();
        for (DocumentaryUnit unit : itemsInScope(scope)) {
            localToGraph.put(unit.getIdentifier(), unit.getId());
        }

        localToGraph.asMap().forEach((localId, graphIds) -> {
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
