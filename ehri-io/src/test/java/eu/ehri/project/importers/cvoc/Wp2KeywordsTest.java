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

package eu.ehri.project.importers.cvoc;

import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;

import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Wp2KeywordsTest extends AbstractImporterTest {
    private static final Logger logger = LoggerFactory.getLogger(Wp2KeywordsTest.class);
    protected final String SKOS_FILE = "cvoc/wp2_skos_keywords.xml";

    @Test
    public void testImportItemsT() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int voccount = toList(vocabulary.getConcepts()).size();
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);
        log.printReport();

//        printGraph(graph);
        /*  How many new nodes will have been created? We should have
         * 388 more Concepts
       	 * 368 more english ConceptDescription
         * 29 german
         * 381 czech
	 * 389 more import Event links (388 for every Unit, 1 for the User)
         * 1 more import Event
         */
        
        int afterNodeCount = count + 1556;
        assertEquals(afterNodeCount, getNodeCount(graph));
        assertEquals(voccount + 388, toList(vocabulary.getConcepts()).size());

        Concept concept = graph.frame(getVertexByIdentifier(graph, "KEYWORD.JMP.847"), Concept.class);
        assertEquals("KEYWORD.JMP.847", concept.getIdentifier());
        for(Concept parent : concept.getBroaderConcepts()){
            assertEquals("KEYWORD.JMP.103", parent.getIdentifier());
        }

        Concept c103 = graph.frame(getVertexByIdentifier(graph, "KEYWORD.JMP.103"), Concept.class);
        boolean found847 = false;
        for(Concept child : c103.getNarrowerConcepts()){
            if(child.getIdentifier().equals("KEYWORD.JMP.847"))
                found847=true;
        }
        assertTrue(found847);
        
        // Check permission scopes
        for (Accessible e : actionManager.getLatestGlobalEvent().getSubjects()) {
            assertEquals(vocabulary, e.getPermissionScope());
        }
    }
}

