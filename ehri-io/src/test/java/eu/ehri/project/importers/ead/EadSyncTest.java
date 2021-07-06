package eu.ehri.project.importers.ead;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;


public class EadSyncTest extends AbstractImporterTest {

    private Repository repo;
    private SaxImportManager importManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InputStream ios = ClassLoader.getSystemResourceAsStream("hierarchical-ead.xml");
        repo = manager.getEntity("r1", Repository.class);
        importManager = saxImportManager(EadImporter.class, EadHandler.class)
                .withScope(repo)
                .withUpdates(true);
        importManager.importInputStream(ios, "Initial setup");
    }

    @Test
    public void testRepositorySync() throws Exception {
        Set<String> excludes = Sets.newHashSet("c1", "c2", "c3", "c4", "nl-r1-m19");
        String logMessage = "Test sync 2";
        SyncLog log = runSync(repo, excludes, logMessage, "hierarchical-ead-sync-test.xml");
        checkSync(repo, logMessage, log);
        assertTrue(manager.exists("c1"));
    }

    @Test
    public void testUnitSync() throws Exception {
        DocumentaryUnit scope = manager.getEntity("nl-r1-ctop_level_fonds", DocumentaryUnit.class);
        Set<String> excludes = Sets.newHashSet();
        String logMessage = "Test sync 2";
        SyncLog log = runSync(scope, excludes, logMessage, "hierarchical-ead-sync-test.xml");
        checkSync(scope, logMessage, log);
    }

    @Test
    public void testUnitSyncWithUserGeneratedContent() throws Exception {
        DocumentaryUnit scope = manager.getEntity("nl-r1-ctop_level_fonds", DocumentaryUnit.class);
        Set<String> excludes = Sets.newHashSet();
        Annotation testAnnotation = api(validUser).createAnnotation(
                "nl-r1-ctop_level_fonds-c00001-c00002-2",
                "nl-r1-ctop_level_fonds-c00001-c00002-2.eng-test_desc_id_eng",
                Bundle.of(EntityClass.ANNOTATION, ImmutableMap.of("body", "Test annotation!")),
                Collections.emptyList(),
                Optional.empty());

        String logMessage = "Test sync 2";
        SyncLog log = runSync(scope, excludes, logMessage, "hierarchical-ead-sync-test.xml");
        checkSync(scope, logMessage, log);

        // Check the new item has the annotation
        DocumentaryUnit unit = manager.getEntity(
                "nl-r1-ctop_level_fonds-c00001-c00002-2_parent-c00002_2", DocumentaryUnit.class);
        DocumentaryUnitDescription desc = manager.getEntity(
                "nl-r1-ctop_level_fonds-c00001-c00002-2_parent-c00002_2.eng-test_desc_id_eng", DocumentaryUnitDescription.class);
        assertEquals(unit, testAnnotation.getTargets().iterator().next());
        assertEquals(desc, testAnnotation.getTargetParts().iterator().next());
        // Transfer event prior to delete event...
        SystemEvent transferEvent = api(validUser).actionManager()
                .getLatestGlobalEvent().getPriorEvent();
        assertEquals(EventTypes.modification, transferEvent.getEventType());
        assertEquals(logMessage, transferEvent.getLogMessage());
    }

    @Test
    public void testUnitSyncWithAccessControl() throws Exception {
        DocumentaryUnit scope = manager.getEntity("nl-r1-ctop_level_fonds", DocumentaryUnit.class);
        DocumentaryUnit child = manager.getEntity("nl-r1-ctop_level_fonds-c00001-c00002-2", DocumentaryUnit.class);
        api(validUser).acl().setAccessors(child, Sets.newHashSet(validUser));
        runSync(scope, Sets.newHashSet(), "Test access sync", "hierarchical-ead-sync-test.xml");

        // Check the new item is in the VC
        DocumentaryUnit moved = manager.getEntity(
                "nl-r1-ctop_level_fonds-c00001-c00002-2_parent-c00002_2", DocumentaryUnit.class);
        List<Accessor> accessors = Lists.newArrayList(moved.getAccessors());
        assertEquals(1, accessors.size());
        assertTrue(accessors.contains(validUser));
    }

    @Test
    public void testUnitSyncWithUserVCParent() throws Exception {
        DocumentaryUnit scope = manager.getEntity("nl-r1-ctop_level_fonds", DocumentaryUnit.class);
        DocumentaryUnit child = manager.getEntity("nl-r1-ctop_level_fonds-c00001-c00002-2", DocumentaryUnit.class);
        VirtualUnit vc = manager.getEntity("vu2", VirtualUnit.class);
        vc.addIncludedUnit(child);

        runSync(scope, Sets.newHashSet(), "Test VC sync", "hierarchical-ead-sync-test.xml");

        // Check the new item is in the VC
        DocumentaryUnit moved = manager.getEntity(
                "nl-r1-ctop_level_fonds-c00001-c00002-2_parent-c00002_2", DocumentaryUnit.class);
        List<DocumentaryUnit> included = Lists.newArrayList(vc.getIncludedUnits());
        assertTrue(included.contains(moved));
    }

    @Test(expected = EadSync.EadSyncError.class)
    public void testUnitSyncWithDuplicateIds() throws Exception {
        // Import the data again but with an additional item that duplicates
        // one of the local IDs. If they are non-unique we cannot sync.
        importManager.withUpdates(true).withScope(repo)
                .importInputStream(ClassLoader.getSystemResourceAsStream(
                        "hierarchical-ead-sync-test-bad.xml"),
                        "Adding item with non-unique ID");
        Set<String> excludes = Sets.newHashSet();
        runSync(repo, excludes, "Test sync error", "hierarchical-ead.xml");
    }

    private SyncLog runSync(PermissionScope scope, Set<String> excludes, String logMessage, String ead) throws Exception {
        EadSync sync = new EadSync(graph, api(validUser), scope, validUser, importManager);
        InputStream ios2 = ClassLoader.getSystemResourceAsStream(ead);
        return sync.sync(m -> {
            try {
                return m.importInputStream(ios2, logMessage);
            } catch (InputParseError e) {
                throw new RuntimeException(e);
            }
        }, excludes, logMessage);
    }

    private void checkSync(PermissionScope scope, String logMessage, SyncLog log) {
        assertEquals(Sets.newHashSet("nl-r1-ctop_level_fonds-c00001-c00002-1"), log.deleted());
        assertEquals(Sets.newHashSet("nl-r1-ctop_level_fonds-c00001-c00002-2_parent"), log.created());
        assertEquals(ImmutableMap.of(
                "nl-r1-ctop_level_fonds-c00001-c00002-2",
                "nl-r1-ctop_level_fonds-c00001-c00002-2_parent-c00002_2"
        ), log.moved());
        assertEquals(3, log.log().getUnchanged());
        assertEquals(log.created().size() + log.moved().size(), log.log().getCreated());

        // Check we actually deleted stuff and that the deletion event is in the
        // right place
        assertFalse(manager.exists("nl-r1-ctop_level_fonds-c00001-c00002-2"));
        assertFalse(manager.exists("nl-r1-ctop_level_fonds-c00001-c00002-1"));
        SystemEvent ev = api(validUser).actionManager().getLatestGlobalEvent();
        assertEquals(logMessage, ev.getLogMessage());
        assertEquals(log.deleted().size() + log.moved().size(), ev.subjectCount());
        assertEquals(scope, ev.getEventScope());
    }
}