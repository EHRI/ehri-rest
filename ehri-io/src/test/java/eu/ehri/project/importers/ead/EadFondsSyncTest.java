package eu.ehri.project.importers.ead;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import org.junit.Test;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class EadFondsSyncTest extends AbstractImporterTest {

    @Test
    public void testSync() throws Exception {

        InputStream ios = ClassLoader.getSystemResourceAsStream("hierarchical-ead.xml");
        Repository scope = manager.getEntity("r1", Repository.class);
        SaxImportManager importManager = saxImportManager(EadImporter.class, EadHandler.class)
                .withScope(scope);
        importManager.importInputStream(ios, "Test sync");
        manager.getEntities(EntityClass.DOCUMENTARY_UNIT, DocumentaryUnit.class)
                .forEach(v -> System.out.println(v.getId()));
        DocumentaryUnit fonds = manager.getEntity("nl-r1-ctop_level_fonds", DocumentaryUnit.class);

        EadFondsSync sync = new EadFondsSync(graph, scope, fonds, validUser,
                false, true, EadImporter.class, EadHandler.class, Optional.empty());

        InputStream ios2 = ClassLoader.getSystemResourceAsStream("hierarchical-ead-sync-test.xml");
        SyncLog log = sync.sync(ios2, "Test sync 2");

        System.out.println(log);
        assertEquals(Sets.newHashSet("C00002-1"), log.deleted());
        assertEquals(Sets.newHashSet("C00002-2-parent"), log.created());
        assertEquals(ImmutableMap.of("nl-r1-ctop_level_fonds-c00001-c00002-2", "nl-r1-ctop_level_fonds-c00001-c00002-2_parent-c00002_2"), log.moved());
        assertEquals(3, log.log().getUnchanged());
        assertEquals(2, log.log().getCreated());
    }
}