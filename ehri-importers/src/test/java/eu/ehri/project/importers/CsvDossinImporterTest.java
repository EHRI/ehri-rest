package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.PermissionScope;
import java.io.InputStream;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class CsvDossinImporterTest extends AbstractImporterTest{
    
    private static final Logger logger = LoggerFactory.getLogger(CsvDossinImporterTest.class);
    protected final String SINGLE_EAD = "dossin.csv";
    protected final String TEST_REPO = "r1";

    @Test
    public void testImportItemsT() throws Exception {
        
        PermissionScope ps = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing some Dossin records";
        XmlImportProperties p = new XmlImportProperties("dossin.properties");
//        assertTrue(p.containsProperty("Creator"));
//        assertTrue(p.containsProperty("Language"));

        int count = getNodeCount(graph);
         // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);

        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        ImportLog log = new CsvImportManager(graph, ps, validUser, EadImporter.class).importFile(ios, logMessage);
        // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
        /*
         * null: 5
         * relationship: 4
         * documentaryUnit: 4
         * documentDescription: 4
         * systemEvent: 1
         * datePeriod: 4
         */
//        printGraph(graph);
        assertEquals(count+22, getNodeCount(graph));
        
        DocumentaryUnit unit = graph.frame(
                getVertexByIdentifier(graph,"kd3"),
                DocumentaryUnit.class);
        
        assertNotNull(unit);
        Repository r = unit.getRepository();
        System.out.println(r.getId());
        Repository repo = graph.frame(
                getVertexByIdentifier(graph,TEST_REPO),
                Repository.class);
        assertEquals(repo, r);
        
    }
}
