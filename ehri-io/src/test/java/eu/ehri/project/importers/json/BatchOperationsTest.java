package eu.ehri.project.importers.json;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.ErrorSet;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BatchOperationsTest extends AbstractImporterTest {

    @Test
    public void testBatchImport() throws Exception {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-test.json");
        String logMsg = "Test import";
        PermissionScope scope = manager.getEntity("r1", PermissionScope.class);

        ImportLog log = new BatchOperations(graph)
                .setScope(scope)
                .batchImport(payloadStream, adminUser.as(Actioner.class), Optional.of(logMsg));
        assertTrue(log.hasDoneWork());
        assertEquals(1, log.getCreated());
        DocumentaryUnit newItem = manager.getEntity("nl-r1-test", DocumentaryUnit.class);
        SystemEvent latestEvent = actionManager.getLatestGlobalEvent();
        assertEquals(newItem, latestEvent.getFirstSubject());

        // Test idempotency - running the same batch twice should not affect
        // the state of the graph.
        int nodesBefore = getNodeCount(graph);
        int edgesBefore = getEdgeCount(graph);
        payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-test.json");
        ImportLog log2 = new BatchOperations(graph).setScope(scope).batchImport(payloadStream,
                adminUser.as(Actioner.class), Optional.of(logMsg));
        assertFalse(log2.hasDoneWork());
        int nodesAfter = getNodeCount(graph);
        int edgesAfter = getEdgeCount(graph);
        assertEquals(nodesBefore, nodesAfter);
        assertEquals(edgesBefore, edgesAfter);
    }

    @Test
    public void testBatchImportWithValidationError() throws Exception {
        int nodesBefore = getNodeCount(graph);
        int edgesBefore = getEdgeCount(graph);
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-test-validation-error.json");
        try {
            new BatchOperations(graph).batchImport(payloadStream,
                    adminUser.as(Actioner.class), Optional.of("Test create"));
            fail("Import with validation error succeeded when it should have failed");
        } catch (ValidationError e) {
            assertEquals(1, e.getErrorSet().getDataValue("identifier").size());
        }

        int nodesAfter = getNodeCount(graph);
        int edgesAfter = getEdgeCount(graph);
        assertEquals(nodesBefore, nodesAfter);
        assertEquals(edgesBefore, edgesAfter);
    }

    @Test
    public void testBatchUpdate() throws Exception {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-patch-test.json");
        String logMsg = "Test partial update";
        int nodesBeforePatch = getNodeCount(graph);
        ImportLog log = new BatchOperations(graph).batchUpdate(payloadStream,
                adminUser.as(Actioner.class), Optional.of(logMsg));
        assertTrue(log.hasDoneWork());
        assertEquals(1, log.getUpdated());
        assertEquals(1, log.getUnchanged());
        DocumentaryUnit doc = manager.getEntity("c1", DocumentaryUnit.class);
        DocumentaryUnitDescription desc = manager.getEntity("cd1", DocumentaryUnitDescription.class);
        assertEquals("Documentary Unit 1 CHANGED", desc.getName());
        SystemEvent latestEvent = actionManager.getLatestGlobalEvent();
        assertEquals(logMsg, latestEvent.getLogMessage());
        assertEquals(1, Iterables.size(latestEvent.getSubjects()));
        assertEquals(doc, latestEvent.getFirstSubject());
        // Newly created nodes will be:
        //  - 1 event link
        //  - 1 action link
        //  - 1 event
        //  - 1 version
        assertEquals(nodesBeforePatch + 4, getNodeCount(graph));

        // Test idempotency - running the same batch twice should not affect
        // the state of the graph.
        int nodesBefore = getNodeCount(graph);
        int edgesBefore = getEdgeCount(graph);
        payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-patch-test.json");
        ImportLog log2 = new BatchOperations(graph).batchUpdate(payloadStream,
                adminUser.as(Actioner.class), Optional.of(logMsg));
        assertFalse(log2.hasDoneWork());
        int nodesAfter = getNodeCount(graph);
        int edgesAfter = getEdgeCount(graph);
        assertEquals(nodesBefore, nodesAfter);
        assertEquals(edgesBefore, edgesAfter);
    }

    @Test
    public void testBatchUpdateWithEmptyStream() throws Exception {
        InputStream emptyStream = new ByteArrayInputStream("[]".getBytes());
        int nodesBefore = getNodeCount(graph);
        ImportLog log = new BatchOperations(graph).batchUpdate(emptyStream,
                adminUser.as(Actioner.class), Optional.of("Test partial update"));
        assertFalse(log.hasDoneWork());
        int nodesAfter = getNodeCount(graph);
        assertEquals(nodesBefore, nodesAfter);
    }

    @Test
    public void testBatchUpdateWithValidationError() throws Exception {
        int nodesBefore = getNodeCount(graph);
        int edgesBefore = getEdgeCount(graph);
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-patch-test-validation-error.json");
        try {
            new BatchOperations(graph).batchUpdate(payloadStream,
                    adminUser.as(Actioner.class), Optional.of("Test partial update"));
            fail("Import with validation error succeeded when it should have failed");
        } catch (ValidationError e) {
            Collection<ErrorSet> describes = e.getErrorSet().getRelations("describes");
            assertEquals(1, Lists.newArrayList(describes).get(0).getDataValue("name").size());
        }

        int nodesAfter = getNodeCount(graph);
        int edgesAfter = getEdgeCount(graph);
        assertEquals(nodesBefore, nodesAfter);
        assertEquals(edgesBefore, edgesAfter);
    }

    @Test
    public void testBatchUpdateWithValidationErrorTolerant() throws Exception {
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-patch-test-validation-error.json");
        ImportLog log = new BatchOperations(graph).setTolerant(true).batchUpdate(payloadStream,
                adminUser.as(Actioner.class), Optional.of("Test partial update"));
        assertTrue(log.hasDoneWork());
        assertEquals(1, log.getUpdated());
        assertEquals(1, log.getErrored());
        assertTrue(log.getErrors().containsKey("c2"));
    }

    @Test
    public void testBatchUpdateWithDeserializationError() throws Exception {
        int nodesBefore = getNodeCount(graph);
        InputStream payloadStream = getClass()
                .getClassLoader().getResourceAsStream("import-patch-test-deserialization-error.json");
        try {
            new BatchOperations(graph).batchUpdate(payloadStream,
                    adminUser.as(Actioner.class), Optional.of("Test partial update"));
            fail("Import with deserialization error succeeded when it should have failed");
        } catch (DeserializationError e) {
            // Okay...
        }
        int nodesAfter = getNodeCount(graph);
        assertEquals(nodesBefore, nodesAfter);
    }

    @Test
    public void testBatchDeleteWithVersioning() throws Exception {
        int nodesBefore = getNodeCount(graph);
        int deleted = new BatchOperations(graph).batchDelete(Lists.newArrayList("a1"),
                adminUser.as(Actioner.class), Optional.of("Test delete"));
        assertEquals(1, deleted);
        // By default, total nodes should have increased by 1, since we
        // deleted 3 nodes (item, description, date period) and created 4:
        //  - 1 version node
        //  - 1 event link
        //  - 1 action link
        //  - 1 event
        assertEquals(nodesBefore + 1, getNodeCount(graph));
    }

    @Test
    public void testBatchDeleteWithoutVersioning() throws Exception {
        int nodesBefore = getNodeCount(graph);
        int deleted = new BatchOperations(graph)
                .setVersioning(false)
                .batchDelete(Lists.newArrayList("a1"),
                        adminUser.as(Actioner.class), Optional.of("Test delete"));
        assertEquals(1, deleted);
        // By default, total nodes should have increased by 0, since we
        // deleted 3 nodes (item, description, date period) and created 4:
        //  - 1 event link
        //  - 1 action link
        //  - 1 event
        assertEquals(nodesBefore, getNodeCount(graph));
    }

    @Test
    public void testBatchDeleteWithNoValidIds() throws Exception {
        int nodesBefore = getNodeCount(graph);
        int deleted = new BatchOperations(graph).setTolerant(true).batchDelete(Lists.newArrayList("NOT-AN-ID"),
                adminUser.as(Actioner.class), Optional.of("Test delete"));
        assertEquals(0, deleted);
        assertEquals(nodesBefore, getNodeCount(graph));
    }
}