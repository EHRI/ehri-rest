/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers;

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.events.SystemEvent;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class Wp2JmpEadTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(Wp2JmpEadTest.class);
    protected final String SINGLE_EAD = "wp2_jmp_ead.xml";
    // Depends on hierarchical-ead.xml
    protected final String C1 = "COLLECTION.JMP.SHOAH/T/2";
    protected final String C2 = "COLLECTION.JMP.SHOAH/T/2/A";
    protected final String C3 = "COLLECTION.JMP.SHOAH/T/2/A/1";
    protected final String C6 = "DOCUMENT.JMP.SHOAH/T/2/A/1a/028";
    // <test-country>-<test-repo>-<__ID__>
    protected final String C6_ID = "nl-r1-collection_jmp_shoah_t-2-a-1-a-028-document_jmp_shoah_t_2_a_1a_028";
    protected final String FONDS = "COLLECTION.JMP.SHOAH/T";

    @Test
    public void testImportItemsT() throws Exception {

        final String logMessage = "Importing JMP EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        importManager = new SaxImportManager(graph, repository, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("wp2ead.properties"));
        
        importManager.setTolerant(Boolean.TRUE);
        
        ImportLog log = importManager.importFile(ios, logMessage);

//        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 7 more DocumentaryUnits fonds C1 C2 C3 4,5,6
        // - 7 more DocumentDescription
        // - 0 more DatePeriod 0 0 1 
        // - 3 UndeterminedRelationship, 0 0 0 11
        // - 8 more import Event links (4 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 0 Annotation as resolved relationship 
        // - 1 unknownProperty


        int newCount = count + 27 ;
        assertEquals(newCount, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, FONDS);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit fonds = graph.frame(getVertexByIdentifier(graph, FONDS), DocumentaryUnit.class);

        // check the child items
        DocumentaryUnit c1 = graph.frame(getVertexByIdentifier(graph, C1), DocumentaryUnit.class);
        DocumentaryUnit c2 = graph.frame(getVertexByIdentifier(graph, C2), DocumentaryUnit.class);
        DocumentaryUnit c3 = graph.frame(getVertexByIdentifier(graph, C3), DocumentaryUnit.class);

        assertEquals(fonds, c1.getParent());
        assertEquals(c1, c2.getParent());
        assertEquals(c2, c3.getParent());

        DocumentaryUnit c6ByIdentifier = graph.frame(getVertexByIdentifier(graph, C6), DocumentaryUnit.class);
        logger.debug(c6ByIdentifier.getId());
        DocumentaryUnit c6ById = graph.frame(getVertexById(graph, C6_ID), DocumentaryUnit.class);
        assertEquals(c6ByIdentifier, c6ById);

        // Ensure the import action has the right number of subjects.
        //        Iterable<Action> actions = unit.getHistory();
        // Check we've created 6 items
        assertEquals(7, log.getCreated());
        assertTrue(log.getAction() != null);
        assertEquals(logMessage, log.getAction().getLogMessage());

//        //assert keywords are matched to cvocs
//        assertTrue(toList(c6.getLinks()).size() > 0);
//        for(Link a : c6.getLinks()){
//            logger.debug(a.getLinkType());
//        }
        
//        languages
        for(DocumentDescription d : c2.getDocumentDescriptions()){
            for(String key : d.asVertex().getPropertyKeys()){
                System.out.println(key);
            }
            assertEquals("deu", d.asVertex().getProperty("languageOfMaterial").toString());
        }

        List<AccessibleEntity> subjects = toList(log.getAction().getSubjects());
        for (AccessibleEntity subject : subjects) {
            logger.info("identifier: " + subject.getId());
        }

        assertEquals(7, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check permission scopes
        assertEquals(repository, fonds.getPermissionScope());
        assertEquals(fonds, c1.getPermissionScope());
        assertEquals(c1, c2.getPermissionScope());
        assertEquals(c2, c3.getPermissionScope());
        
        // Check the author of the description
        for (DocumentDescription d : fonds.getDocumentDescriptions()){
            assertEquals("Shoah History Department, Jewish Museum in Prague", d.asVertex().getProperty("processInfo"));
        }

        // Check the importer is Idempotent
        ImportLog log2 = importManager.importFile(ClassLoader.getSystemResourceAsStream(SINGLE_EAD), logMessage);
        assertEquals(7, log2.getUnchanged());
        //assertEquals(0, log2.getChanged());
        assertEquals(newCount, getNodeCount(graph));
    }
}
