/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.PermissionScope;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author linda
 */
public class Bbwo2HandlerTest extends AbstractImporterTest {

    protected final String TEST_REPO = "r1";
    protected final String XMLFILE_NL = "bbwo2.xml";
    protected final String ARCHDESC = "1505";
    DocumentaryUnit archdesc;
    int origCount = 0;

    @Test
    public void bbwo2Test() throws ItemNotFound, IOException, ValidationError, InputParseError {

        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example BBWO2 DC";
        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);

        origCount = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(XMLFILE_NL);
        ImportLog log = new SaxImportManager(graph, agent, validUser, EadImporter.class, DcEuropeanaHandler.class, new XmlImportProperties("dceuropeana.properties")).importFile(ios, logMessage);
//        printGraph(graph);
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);

        /**
         * How many new nodes will have been created? We should have 
         * 1 more DocumentaryUnits (record) 
         * 1 more DocumentDescription 
         * 1 more DatePeriod 
         * 4 relations (3 more subject + 1 more Place )
         * 2 more import Event links (1 for each Unit, 1 for the User) 
         * 1 more import Event 
         */
        int newCount = origCount + 10;
        assertEquals(newCount, getNodeCount(graph));

        DocumentaryUnit archdesc = graph.frame(getVertexByIdentifier(graph, ARCHDESC), DocumentaryUnit.class);
        for (DocumentDescription d : archdesc.getDocumentDescriptions()) {
            assertEquals("More refugee children arrive from Germany - in time ...", d.getName());
            assertEquals("1505", d.asVertex().getProperty("sourceFileId"));
            System.out.println(d.getId() + " - " + archdesc.asVertex().getProperty("identifier"));
        }
    }
}
