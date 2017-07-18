package eu.ehri.project.importers.ead;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.io.InputStream;
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

    public SyncLog sync(InputStream ios, String logMessage) throws ArchiveException, InputParseError, ValidationError, IOException {

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
        ImportLog log = manager.importInputStream(ios, logMessage);

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
        for (DocumentaryUnit item : getAllChildren(scope)) {
            for (DocumentaryUnit other : getAllChildren(scope)) {
                if (item.getIdentifier().equals(other.getIdentifier())
                        && item.getId().compareTo(other.getId()) < 0) {
                    if (lookup.containsKey(item.getId())) {
                        moved.put(item.getId(), other.getId());
                    } else if (lookup.containsKey(other.getId())) {
                        moved.put(other.getId(), item.getId());
                    } else {
                        throw new RuntimeException(
                                "Unexpected situation: 'moved' item not found in before-set... " + item.getIdentifier());
                    }
                }
            }
        }
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
