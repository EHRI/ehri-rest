/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.AuthoritativeItem;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.views.impl.CrudViews;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 *
 * @author linda
 */
public class Wp2TmEadTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(Wp2TmEadTest.class);
    protected final String SINGLE_EAD = "wp2_tm_ead.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml
    protected final String C1_A = "A 11486";
    protected final String C1_B = "A 11487";
    protected final String FONDS = "vzpomínky pro EHRI";

    @Test
    public void testImportItemsT() throws Exception {

        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        Bundle vocabularyBundle = new Bundle(EntityClass.CVOC_VOCABULARY)
                                .withDataValue(Ontology.IDENTIFIER_KEY, "WP2_keywords");
        Bundle conceptBundle = new Bundle(EntityClass.CVOC_CONCEPT)
                                .withDataValue(Ontology.IDENTIFIER_KEY, "KEYWORD.JMP.191");
        AuthoritativeSet vocabulary = new CrudViews<AuthoritativeSet>(graph, AuthoritativeSet.class).create(vocabularyBundle, validUser);
        logger.debug(vocabulary.getId());
        AuthoritativeItem concept_191 = new CrudViews<Concept>(graph, Concept.class).create(conceptBundle, validUser); 
        vocabulary.addAuthoritativeItem(concept_191);
        
        
        Vocabulary vocabularyTest = manager.getFrame("wp2-keywords", Vocabulary.class);
        assertNotNull(vocabularyTest);
        
        final String logMessage = "Importing Terezin Memorial EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        XmlImportManager importManager = new SaxImportManager(graph, agent, validUser, Wp2EadImporter.class, Wp2EadHandler.class)
                .setTolerant(Boolean.TRUE);
        ImportLog log = importManager.importFile(ios, logMessage);

        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 3 more DocumentaryUnits fonds 2C1 
        // - 3 more DocumentDescription
        // - 4 UndeterminedRelationship, 0 3 1
        // - 4 more import Event links (3 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 1 Annotation as resolved relationship 

        //TODO: the dates are in a weird format, so they are not recognized
        // (- 2 more DatePeriod 1 0 1 )

        int newCount = count + 16;
        assertEquals(newCount, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, FONDS);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit fonds = graph.frame(getVertexByIdentifier(graph, FONDS), DocumentaryUnit.class);

        // check the child items
        DocumentaryUnit c1_a = graph.frame(getVertexByIdentifier(graph, C1_A), DocumentaryUnit.class);
        DocumentaryUnit c1_b = graph.frame(getVertexByIdentifier(graph, C1_B), DocumentaryUnit.class);

        assertEquals(fonds, c1_a.getParent());
        assertEquals(fonds, c1_b.getParent());
        
        // Ensure the import action has the right number of subjects.
        //        Iterable<Action> actions = unit.getHistory();
        // Check we've created 6 items
        assertEquals(3, log.getCreated());
        assertTrue(log.getAction() instanceof SystemEvent);
        assertEquals(logMessage, log.getAction().getLogMessage());

        //assert keywords are matched to cvocs
        assertTrue(toList(c1_a.getLinks()).size() > 0);
        for(Link a : c1_a.getLinks()){
            logger.debug(a.getLinkType());
        }

        List<AccessibleEntity> subjects = toList(log.getAction().getSubjects());
        for (AccessibleEntity subject : subjects) {
            logger.info("identifier: " + subject.getId());
        }

        assertEquals(3, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check permission scopes
        assertEquals(agent, fonds.getPermissionScope());
        assertEquals(fonds, c1_a.getPermissionScope());
        assertEquals(fonds, c1_b.getPermissionScope());
        
        // Check the author of the description
        for (DocumentDescription d : c1_a.getDocumentDescriptions()){
            assertEquals(Wp2EadImporter.WP2AUTHOR, d.asVertex().getProperty(Wp2EadImporter.PROPERTY_AUTHOR));
        }

        // Check the importer is Idempotent
        ImportLog log2 = importManager.importFile(ClassLoader.getSystemResourceAsStream(SINGLE_EAD), logMessage);
        assertEquals(3, log2.getUnchanged());
        //assertEquals(0, log2.getChanged());
        assertEquals(newCount, getNodeCount(graph));
    }
}
