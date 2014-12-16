package eu.ehri.project.importers.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.LinkableEntity;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.ConceptDescription;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.views.Query;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class JoodsRaadTest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(JoodsRaadTest.class);
    protected final String EHRI_SKOS_TERM = "cvoc/joods_raad.xml";
    protected final String NIOD_SKOS_TERM = "cvoc/niod-joodseraad.xml";

    @Test
    public void testRelatedWithinScheme() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getFrame("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int voccount = toList(vocabulary.getConcepts()).size();
        InputStream ios = ClassLoader.getSystemResourceAsStream(EHRI_SKOS_TERM);
        assertNotNull(ios);
        
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
       

//        printGraph(graph);
        /*  How many new nodes will have been created? We should have
         * 3 more Concepts
       	 * 7 more ConceptDescription
	 * 4 more import Event links (8 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count + 15, getNodeCount(graph));
        assertEquals(voccount + 3, toList(vocabulary.getConcepts()).size());

        // get a top concept
        String skosConceptId = "698";
        Query<Concept> query = new Query<Concept>(graph, Concept.class);
        // Query for document identifier.
        List<Concept> list = toList(query.setLimit(1).page(
                Ontology.IDENTIFIER_KEY, skosConceptId, validUser));

        assertEquals(1, toList(list.get(0).getBroaderConcepts()).size());

        // Check permission scopes
        for (AccessibleEntity e : log.getAction().getSubjects()) {
            assertEquals(vocabulary, e.getPermissionScope());
        }
        Concept term698 = manager.getFrame("cvoc1-698", Concept.class);
        boolean found=false;
        for(Concept rel : term698.getRelatedConcepts()){
            found=true;
            assertEquals("307", rel.getIdentifier());
        }
        assertTrue(found);
    }

   @Test
    public void testCloseMatchOutsideScheme() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary cvoc1 = manager.getFrame("cvoc1", Vocabulary.class);
        InputStream ios = ClassLoader.getSystemResourceAsStream(EHRI_SKOS_TERM);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, cvoc1);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);

        
        int count = getNodeCount(graph);
        assertNotNull(ios);
        
        Vocabulary cvoc2 = manager.getFrame("cvoc2", Vocabulary.class);
        InputStream niod_ios = ClassLoader.getSystemResourceAsStream(NIOD_SKOS_TERM);
        assertNotNull(niod_ios);
        SkosImporter niod_importer = SkosImporterFactory.newSkosImporter(graph, validUser, cvoc2);
        niod_importer.setTolerant(true);
        int voccount = toList(cvoc2.getConcepts()).size();

        // Before...
        List<VertexProxy> graphState1 = getGraphState(graph);
        niod_importer.importFile(niod_ios, logMessage);
        // After...
        List<VertexProxy> graphState2 = getGraphState(graph);
        GraphDiff diff = diffGraph(graphState1, graphState2);
        diff.printDebug(System.out);
       

//        printGraph(graph);
        /*  How many new nodes will have been created? We should have
         * 1 more Concepts
       	 * 1 more ConceptDescription
         * 1 more Link
	 * 2 more import Event links (8 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count + 6, getNodeCount(graph));
        assertEquals(voccount + 1, toList(cvoc2.getConcepts()).size());

        Concept term698 = manager.getFrame("cvoc1-698", Concept.class);
        boolean found=false;
        for(Concept rel : term698.getRelatedConcepts()){
            found=true;
            assertEquals("307", rel.getIdentifier());
        }
        assertTrue(found);

        Concept termJR = manager.getFrame("cvoc2-joodse-raad", Concept.class);
        
        for(Link desc : termJR.getLinks()){
            assertTrue(desc.asVertex().getPropertyKeys().contains("type"));
            assertEquals("associate", desc.asVertex().getProperty("type"));
            assertTrue(desc.asVertex().getPropertyKeys().contains("skos"));
            assertEquals("exactMatch", desc.asVertex().getProperty("skos"));
        }

        Concept concept698 = manager.getFrame("cvoc1-698", Concept.class);
        found=false;
        for(Edge e : concept698.asVertex().getEdges(Direction.IN, "hasLinkTarget")){
            Link l = graph.frame(e.getVertex(Direction.OUT), Link.class);
            boolean bothTargetsFound = false;
            for (LinkableEntity entity : l.getLinkTargets()){
                if(entity.equals(termJR))
                    bothTargetsFound=true;
            }
            assertTrue(bothTargetsFound);
            for(String k : e.getVertex(Direction.OUT).getPropertyKeys()){
                logger.debug(k + ":" + e.getVertex(Direction.OUT).getProperty(k));
            }
            int countHasLinkTarget=0;
            for(Edge out : e.getVertex(Direction.OUT).getEdges(Direction.OUT)){
                logger.debug(out.getLabel());
                if(out.getLabel().equals("hasLinkTarget")){
                    countHasLinkTarget++;
                }
            }
            assertEquals(2, countHasLinkTarget);
            found=true;
        }
        assertTrue(found);
                
        found=false;
        for(Concept rel : termJR.getRelatedConcepts()){
            for(String key : rel.asVertex().getPropertyKeys()){
                logger.debug(key + "" + rel.asVertex().getProperty(key));
            }
            found=true;
        }
        assertFalse(found);
}


}
