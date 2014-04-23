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
import eu.ehri.project.models.VirtualUnit;
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
public class Wp2YvEadTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(Wp2YvEadTest.class);
    protected final String SINGLE_EAD = "wp2_yv_ead.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml
    protected final String C1 = "O.64.2-A.";
    protected final String C2 = "O.64.2-A.A.";
    protected final String C3 = "3685529";
    protected final String FONDS = "O.64.2";

    @Test
    public void testImportItemsT() throws Exception {

        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        Bundle vocabularyBundle = new Bundle(EntityClass.CVOC_VOCABULARY)
                                .withDataValue(Ontology.IDENTIFIER_KEY, "WP2_keywords");
        Bundle conceptBundle = new Bundle(EntityClass.CVOC_CONCEPT)
                                .withDataValue(Ontology.IDENTIFIER_KEY, "KEYWORD.JMP.288");
        AuthoritativeSet vocabulary = new CrudViews<AuthoritativeSet>(graph, AuthoritativeSet.class).create(vocabularyBundle, validUser);
        logger.debug(vocabulary.getId());
        AuthoritativeItem concept_288 = new CrudViews<Concept>(graph, Concept.class).create(conceptBundle, validUser); 
        vocabulary.addAuthoritativeItem(concept_288);
        
        
        Vocabulary vocabularyTest = manager.getFrame("wp2-keywords", Vocabulary.class);
        assertNotNull(vocabularyTest);
        
        final String logMessage = "Importing Yad Vashem EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        SaxImportManager importManager = new SaxImportManager(graph, agent, validUser, VirtualCollectionEadImporter.class, VirtualCollectionEadHandler.class);
        
        importManager.setTolerant(Boolean.TRUE);
        VirtualUnit virtualcollection = graph.frame(getVertexByIdentifier(graph, "vc1"), VirtualUnit.class);
        importManager.setVirtualCollection(virtualcollection);
        
        ImportLog log = importManager.importFile(ios, logMessage);

        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 4 more DocumentaryUnits fonds C1 C2 C3 
        // - 4 more VirtualUnits
        // - 4 more DocumentDescription
        // - 1 more DatePeriod 0 0 1 
        // - 11 UndeterminedRelationship, 0 0 0 11
        // - 5 more import Event links (4 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 1 Annotation as resolved relationship 


        int newCount = count + 27+4;
        assertEquals(newCount, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, FONDS);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit fonds = graph.frame(getVertexByIdentifier(graph, FONDS), DocumentaryUnit.class);

        // check the child items
        DocumentaryUnit c1 = graph.frame(getVertexByIdentifier(graph, C1), DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(getVertexByIdentifier(graph, C2), DocumentaryUnit.class);
        DocumentaryUnit c3 = graph.frame(getVertexByIdentifier(graph, C3), DocumentaryUnit.class);
        VirtualUnit v_c3 = graph.frame(getVertexByIdentifier(graph, VirtualCollectionEadImporter.VIRTUAL_PREFIX+C3), VirtualUnit.class);

        assertEquals(fonds, c1.getParent());
        assertEquals(c1, c2.getParent());
        assertEquals(c2, c3.getParent());
        
        // Ensure the import action has the right number of subjects.
        //        Iterable<Action> actions = unit.getHistory();
        // Check we've created 6 items
        assertEquals(4, log.getCreated());
        assertTrue(log.getAction() instanceof SystemEvent);
        assertEquals(logMessage, log.getAction().getLogMessage());

        //assert keywords are matched to cvocs
        assertTrue(toList(v_c3.getLinks()).size() > 0);
        for(Link a : v_c3.getLinks()){
            logger.debug(a.getLinkType());
        }

        List<AccessibleEntity> subjects = toList(log.getAction().getSubjects());
        for (AccessibleEntity subject : subjects) {
            logger.info("identifier: " + subject.getId());
        }

        assertEquals(4, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check permission scopes
        assertEquals(agent, fonds.getPermissionScope());
        assertEquals(fonds, c1.getPermissionScope());
        assertEquals(c1, c2.getPermissionScope());
        assertEquals(c2, c3.getPermissionScope());
        
        // Check the author of the description
        for (DocumentDescription d : c1.getDocumentDescriptions()){
            assertEquals(VirtualCollectionEadImporter.WP2AUTHOR, d.asVertex().getProperty(VirtualCollectionEadImporter.PROPERTY_AUTHOR));
        }

        // Check the importer is Idempotent
        ImportLog log2 = importManager.importFile(ClassLoader.getSystemResourceAsStream(SINGLE_EAD), logMessage);
        assertEquals(4, log2.getUnchanged());
        //assertEquals(0, log2.getChanged());
        assertEquals(newCount, getNodeCount(graph));
    }
}
