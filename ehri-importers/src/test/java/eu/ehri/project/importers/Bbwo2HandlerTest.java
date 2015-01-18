package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.UndeterminedRelationship;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.LinkableEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.CrudViews;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class Bbwo2HandlerTest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(Bbwo2HandlerTest.class);
    protected final String TEST_REPO = "r1";
    protected final String XMLFILE_NL = "bbwo2.xml";
    protected final String ARCHDESC = "1505";
    DocumentaryUnit archdesc;
    int origCount = 0;

    @Test
    public void bbwo2Test() throws ItemNotFound, IOException, ValidationError, InputParseError, PermissionDenied, IntegrityError {

        PermissionScope agent = manager.getFrame(TEST_REPO, PermissionScope.class);
        final String logMessage = "Importing an example BBWO2 DC";
        
        //id="joodse-raad" source="niod-trefwoorden" term="Kinderen"
        Bundle vocabularyBundle = new Bundle(EntityClass.CVOC_VOCABULARY)
                                .withDataValue(Ontology.IDENTIFIER_KEY, "niod-trefwoorden")
                                .withDataValue(Ontology.NAME_KEY, "NIOD Keywords");
        Bundle conceptBundle = new Bundle(EntityClass.CVOC_CONCEPT)
                                .withDataValue(Ontology.IDENTIFIER_KEY, "joodse-raad");
        Vocabulary vocabulary = new CrudViews<Vocabulary>(graph, Vocabulary.class).create(vocabularyBundle, validUser);
        Concept concept_716 = new CrudViews<Concept>(graph, Concept.class).create(conceptBundle, validUser); 
        vocabulary.addItem(concept_716);
        
        
        Vocabulary vocabularyTest = manager.getFrame("niod-trefwoorden", Vocabulary.class);
        assertNotNull(vocabularyTest);

        
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
         * null: 2
         * relationship: 4
         * documentaryUnit: 1
         * link: 1
         * property: 1
         * documentDescription: 1
         * systemEvent: 1
         * 
         */
        int newCount = origCount + 10;
        assertEquals(newCount, getNodeCount(graph));

        DocumentaryUnit archdesc = graph.frame(getVertexByIdentifier(graph, ARCHDESC), DocumentaryUnit.class);
        assertNotNull(archdesc);
        for (DocumentDescription d : archdesc.getDocumentDescriptions()) {
            assertEquals("More refugee children arrive from Germany - in time ...", d.getName());
            assertEquals("1505", d.asVertex().getProperty("sourceFileId"));
            logger.debug("id:"+d.getId() + " - identifier:" + archdesc.asVertex().getProperty("identifier"));
        }

        for(Concept concept : vocabularyTest.getConcepts()){
            logger.debug("concept:"+concept.getIdentifier());
        }
        
        boolean passTest = false;
        DocumentaryUnit person = manager.getFrame("nl-r1-1505", DocumentaryUnit.class);
        for (Description d : person.getDescriptions()) {
            for (UndeterminedRelationship rel : d.getUndeterminedRelationships()) {
                if (rel.getRelationshipType().equals("subjectAccess")) {
                    if (rel.getName().equals("kinderen")) {
                        assertEquals(1, toList(rel.getLinks()).size());
                        for (Link link : rel.getLinks()) {
                            boolean conceptFound = false;
                            for (LinkableEntity le : link.getLinkTargets()) {
                                if (le.getType().equals("cvocConcept")) {
                                    assertEquals(le, concept_716);
                                    conceptFound = true;
                                }
                            }
                            assertTrue(conceptFound);
                            passTest=true;
                            logger.debug(link.getLinkType());
                            for (String key : link.asVertex().getPropertyKeys()) {
                                logger.debug(key + ":" + link.asVertex().getProperty(key));
                            }
                        }
                    }
                }
            }
        }
        assertTrue(passTest);
    }
}
