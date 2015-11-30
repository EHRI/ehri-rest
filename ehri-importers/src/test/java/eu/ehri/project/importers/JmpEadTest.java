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
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentDescription;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.AccessibleEntity;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class JmpEadTest extends AbstractImporterTest {

    private static final Logger logger = LoggerFactory.getLogger(JmpEadTest.class);
    protected final String SINGLE_EAD = "JMP20141117.xml";
    // Depends on fixtures
    protected final String TEST_REPO = "r1";
    // Depends on hierarchical-ead.xml

    protected final String FONDS = "COLLECTION.JMP.ARCHIVE/NAD3";

    @Test
    public void testImportItemsT() throws Exception {

        Repository agent = manager.getFrame(TEST_REPO, Repository.class);
        
        final String logMessage = "Importing JMP EAD";

        int count = getNodeCount(graph);
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        SaxImportManager importManager = new SaxImportManager(graph, agent, validUser, EadImporter.class, EadHandler.class, new XmlImportProperties("wp2ead.properties"));
        
        importManager.setTolerant(Boolean.TRUE);
        
        ImportLog log = importManager.importFile(ios, logMessage);

//        printGraph(graph);
        // How many new nodes will have been created? We should have
        // - 1 more DocumentaryUnits fonds C1 C2 C3 4,5,6
        // - 1 more DocumentDescription
        // - 0 more DatePeriod 0 0 1 
        // - 3 UndeterminedRelationship, 0 0 0 11
        // - 2 more import Event links (4 for every Unit, 1 for the User)
        // - 1 more import Event
        // - 0 Annotation as resolved relationship 
        // - 1 unknownProperty


        printGraph(graph);
        int newCount = count + 12 ;
        assertEquals(newCount, getNodeCount(graph));

        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, FONDS);
        assertTrue(docs.iterator().hasNext());
        DocumentaryUnit fonds = graph.frame(getVertexByIdentifier(graph, FONDS), DocumentaryUnit.class);

        for(DocumentDescription d : fonds.getDocumentDescriptions()){
            for(String key : d.getPropertyKeys()){
                System.out.println(key);
            }
            System.out.println(d.getProperty("languageOfMaterial"));
            assertTrue(d.getProperty("languageOfMaterial").toString().startsWith("[deu"));
        }

        List<AccessibleEntity> subjects = toList(actionManager.getLatestGlobalEvent().getSubjects());
        for (AccessibleEntity subject : subjects) {
            logger.info("identifier: " + subject.getId());
        }

        //huh, should be 7?
        assertEquals(1, subjects.size());
        assertEquals(log.getChanged(), subjects.size());

        // Check permission scopes
        assertEquals(agent, fonds.getPermissionScope());
    }
}
