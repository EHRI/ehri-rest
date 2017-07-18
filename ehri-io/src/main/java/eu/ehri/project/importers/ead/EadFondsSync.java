package eu.ehri.project.importers.ead;

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.managers.ImportManager;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import org.apache.commons.compress.archivers.ArchiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EadFondsSync {

    private static final Logger logger = LoggerFactory.getLogger(EadFondsSync.class);

    private final FramedGraph<?> graph;
    private final PermissionScope scope;
    private final Actioner actioner;
    private final SaxImportManager importManager;
    private final Set<String> excludes;

    public EadFondsSync(
            FramedGraph<?> graph,
            PermissionScope scope, Actioner actioner, SaxImportManager importManager,
            Set<String> excludes) {
        this.graph = graph;
        this.scope = scope;
        this.actioner = actioner;
        this.importManager = importManager;
        this.excludes = excludes;
    }

    public interface IngestOperation {
        ImportLog run(ImportManager manager) throws ArchiveException, InputParseError, ValidationError, IOException;
    }

    public SyncLog sync(IngestOperation op) throws ArchiveException, InputParseError, ValidationError, IOException {

        // Get a mapping of __id to identifier within the scope,
        // pre-ingest...
        Map<String, String> lookup = Maps.newHashMap();
        for (DocumentaryUnit unit : getAllChildren(scope)) {
            lookup.put(unit.getId(), unit.getIdentifier());
        }

        // Remove anything specifically excludes...
        excludes.forEach(lookup::remove);

        logger.debug("ITEMS IN BEFORE MAP: {}", lookup.size());

        // Keep track of new, moved, updated local identifiers.
        List<String> newIdentifiers = Lists.newArrayList();

        // Run the import!
        ImportManager manager = importManager
                .withCallback(m -> newIdentifiers.add(m.getNode().as(DocumentaryUnit.class).getIdentifier()));
        ImportLog log = op.run(manager);

        Set<String> allBefore = Sets.newHashSet(lookup.values());
        Set<String> allNew = Sets.newHashSet(newIdentifiers);
        allNew.removeAll(allBefore);
        allBefore.removeAll(newIdentifiers);

        logger.debug("NUMBER TO BE DELETED: {}", allBefore.size());
        logger.debug("NUMBER NEWLY CREATED: {}", allNew.size());

        // Find moved items...
        // This gets us a map of old __id to new __id
        Map<String, String> oldToNew = findMovedItems(scope, lookup);
        logger.debug("MOVED ITEMS: {}", oldToNew.size());

        logger.debug("Committing import transaction...");

        return new SyncLog(log, allBefore, oldToNew, allNew);
    }

    private Map<String, String> findMovedItems(PermissionScope scope, Map<String, String> lookup) {
        Map<String, String> moved = Maps.newHashMap();

        logger.debug("Starting moved item scan...");
        long start = System.nanoTime();
        // NB: This method of finding moved items uses a lot of memory for big
        // repositories, but is dramatically faster than the alternative.
        Multimap<String, String> scan = LinkedHashMultimap.create();
        getAllChildren(scope).forEach(item -> scan.put(item.getIdentifier(), item.getId()));

        scan.asMap().entrySet().forEach(e -> {
            List<String> ids = Lists.newArrayList(e.getValue());
            if (ids.size() > 1) {
                Preconditions.checkState(ids.size() == 2,
                        "Unexpected situation in EAD sync. Item " + e.getKey() +
                                " cannot be unique since after sync ingest there are it exists in more than two places: " + ids);
                String first = ids.get(0);
                String second = ids.get(1);
                if (lookup.containsKey(first)) {
                    moved.put(first, second);
                } else if (lookup.containsKey(second)) {
                    moved.put(second, first);
                } else {
                    throw new RuntimeException(
                            "Unexpected situation: 'moved' item not found in before-set... " + e.getKey());
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
