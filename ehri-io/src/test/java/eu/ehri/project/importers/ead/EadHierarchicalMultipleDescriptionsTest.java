package eu.ehri.project.importers.ead;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import org.junit.Test;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test updating a hierarchical item with an alternate language
 * description.
 */
public class EadHierarchicalMultipleDescriptionsTest extends AbstractImporterTest {

    @Test
    public void testUpdateDescription() throws Exception {
        int count = getNodeCount(graph);
        ImportOptions options = ImportOptions.basic();
        SaxImportManager importManager = saxImportManager(EadImporter.class, EadHandler.class, options);
        try (InputStream ios = ClassLoader.getSystemResourceAsStream("photos-en-00.xml")) {
            importManager.importInputStream(ios, "Import English description");
        }

        // Added: 1 event, 4 event links, 3 doc unit, 3 description, 1 date period, 7 access points
        assertEquals(count + 19, getNodeCount(this.graph));

        DocumentaryUnit unit = manager.getEntity("nl-r1-photos", DocumentaryUnit.class);
        assertEquals(1, Iterables.size(unit.getDocumentDescriptions()));

        SaxImportManager importManager2 = saxImportManager(EadImporter.class, EadHandler.class, options.withUpdates(true));
        try (InputStream ios2 = ClassLoader.getSystemResourceAsStream("photos-uk-00.xml")) {
            importManager2.importInputStream(ios2, "Import Ukrainian description");
        }

        // Should have added: 1 event, 4 event links , 3 new descriptions, 1 new date period, 7 access points
        assertEquals(count + 19 + 16, getNodeCount(this.graph));

        unit = manager.getEntity("nl-r1-photos", DocumentaryUnit.class);
        printProps(System.out, unit, "");
        List<DocumentaryUnitDescription> descriptions = Lists.newArrayList(unit.getDocumentDescriptions());
        assertEquals(2, descriptions.size());

        // Check child descriptions have an `extentAndMedium` property...
        DocumentaryUnit child = manager.getEntity("nl-r1-photos-14_pict-8", DocumentaryUnit.class);
        List<DocumentaryUnitDescription> childDescriptions = Lists.newArrayList(child.getDocumentDescriptions());
        assertEquals(2, childDescriptions.size());
        for (DocumentaryUnitDescription childDesc : childDescriptions) {
            assertNotNull(childDesc.getProperty("extentAndMedium"));
        }
    }
}
