package eu.ehri.project.importers.cvoc;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;

import java.io.InputStream;
import org.junit.Test;


import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class EventsSkosImporterTest extends AbstractImporterTest {

    protected final String EVENT_SKOS = "cvoc/ehri-events.rdf";
    protected final String EHRI_SKOS_TERM = "cvoc/joods_raad.xml";
    final String logMessage = "Importing a single skos: " + EVENT_SKOS;

  
    @Test
    public void testImportItemsT() throws Exception {

        int count = getNodeCount(graph);
        Vocabulary vocabulary = manager.getFrame("cvoc1", Vocabulary.class);
        InputStream ios = ClassLoader.getSystemResourceAsStream(EVENT_SKOS);
//        SkosCoreCvocImporter importer = new SkosCoreCvocImporter(graph, validUser, vocabulary);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        
              // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        ImportLog log = importer.importFile(ios, logMessage);
//        log.printReport();
        // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);


        /** How many new nodes will have been created? We should have
         * relationship: 2
         * null: 3
         * cvocConceptDescription: 2
         * systemEvent: 1
         * cvocConcept: 2
         */
        assertEquals(count + 10, getNodeCount(graph));
//        printGraph(graph);
        
    }
    
    @Test
    public void withOutsideScheme() throws ItemNotFound, IOException, InputParseError, ValidationError{
        Vocabulary cvoc1 = manager.getFrame("cvoc1", Vocabulary.class);
        InputStream ios1 = ClassLoader.getSystemResourceAsStream(EHRI_SKOS_TERM);
        SkosImporter importer1 = SkosImporterFactory.newSkosImporter(graph, validUser, cvoc1);
        importer1.setTolerant(true);
        ImportLog log1 = importer1.importFile(ios1, logMessage);

           int count = getNodeCount(graph);
        Vocabulary vocabulary = manager.getFrame("cvoc2", Vocabulary.class);
        InputStream ios = ClassLoader.getSystemResourceAsStream(EVENT_SKOS);
//        SkosCoreCvocImporter importer = new SkosCoreCvocImporter(graph, validUser, vocabulary);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        
              // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        ImportLog log = importer.importFile(ios, logMessage);
//        log.printReport();
        // After...
       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);


        /** How many new nodes will have been created? We should have
         * link: 2
         * null: 3
         * cvocConceptDescription: 2
         * systemEvent: 1
         * cvocConcept: 2
         */
        assertEquals(count + 10, getNodeCount(graph));
        printGraph(graph);   
        
        Concept termJR = manager.getFrame("cvoc1-tema-866", Concept.class);
        
        boolean found=false;
        for(Link desc : termJR.getLinks()){
            found=true;
            assertTrue(desc.asVertex().getPropertyKeys().contains("type"));
            assertEquals("associate", desc.asVertex().getProperty("type"));
            assertTrue(desc.asVertex().getPropertyKeys().contains("skos"));
            assertEquals("broadMatch", desc.asVertex().getProperty("skos"));
        }
        assertTrue(found);

    }
}
