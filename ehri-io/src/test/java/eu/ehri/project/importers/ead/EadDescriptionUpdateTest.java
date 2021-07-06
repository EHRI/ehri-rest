package eu.ehri.project.importers.ead;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test updating a single item with an alternate language
 * description. An additional description should be added
 * the second time and updated the third.
 */
public class EadDescriptionUpdateTest extends AbstractImporterTest {

    @Test
    public void testUpdateDescription() throws Exception {
        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream("single-ead-multilang-eng.xml");
        ImportOptions options = ImportOptions.basic();
        SaxImportManager importManager = saxImportManager(EadImporter.class, EadHandler.class, options);
        importManager.importInputStream(ios, "Import English description");

        // Added: 1 event, 2 event links, 1 doc unit, 1 description
        assertEquals(count + 5, getNodeCount(graph));

        DocumentaryUnit unit = manager.getEntity("nl-r1-c00001", DocumentaryUnit.class);
        assertEquals(1, Iterables.size(unit.getDocumentDescriptions()));

        InputStream ios2 = ClassLoader.getSystemResourceAsStream("single-ead-multilang-deu.xml");
        SaxImportManager importManager2 = saxImportManager(EadImporter.class, EadHandler.class, options.withUpdates(true));
        importManager2.importInputStream(ios2, "Import German description");

        // Should only have: 1 event, 2 event links , 1 new description
        assertEquals(count + 9, getNodeCount(graph));

        unit = manager.getEntity("nl-r1-c00001", DocumentaryUnit.class);
        List<DocumentaryUnitDescription> descriptions = Lists.newArrayList(unit.getDocumentDescriptions());
        assertEquals(2, descriptions.size());

        // Updating the German description should only create event nodes
        InputStream ios3 = ClassLoader.getSystemResourceAsStream("single-ead-multilang-deu-2.xml");
        importManager2.importInputStream(ios3, "Import German description again");
        // Should only have: 1 event, 2 event links
        assertEquals(count + 12, getNodeCount(graph));
    }
}
