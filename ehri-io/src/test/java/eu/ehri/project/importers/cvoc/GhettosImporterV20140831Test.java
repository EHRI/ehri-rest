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

import eu.ehri.project.api.QueryApi;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.AbstractImporterTest;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class GhettosImporterV20140831Test extends AbstractImporterTest {
    protected final String SKOS_FILE = "cvoc/ghettos.rdf";
    protected final String SKOS_V2_FILE = "cvoc/ghettos-v20140831.rdf.xml";

    @Test
    public void testImportItems() throws Exception {
        final String logMessage = "Importing a SKOS file";

        Vocabulary vocabulary = manager.getEntity("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int voccount = toList(vocabulary.getConcepts()).size();
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);
//        printGraph(graph);

        /*  How many new nodes will have been created? We should have
         * 2 more Concepts
         * 4 more ConceptDescription
         * 4 UndeterminedRelationships
     	 * 3 more import Event links (2 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count + 14, getNodeCount(graph));
        assertEquals(2, log.getCreated());
        assertEquals(voccount + 2, toList(vocabulary.getConcepts()).size());

        // get a top concept
        String skosConceptId = "0";
        QueryApi query = api(validUser).query();

        // Query for document identifier.
        List<Concept> list = toList(query.setLimit(1).page(
                Ontology.IDENTIFIER_KEY, skosConceptId, Concept.class));

        Concept ghetto0 = manager.getEntity("cvoc1-0", Concept.class);
        //  <geo:lat>52.43333333333333</geo:lat>
        //	<geo:long>20.716666666666665</geo:long>
        assertEquals("52.43333333333333", ghetto0.getProperty("latitude"));
        assertEquals("20.716666666666665", ghetto0.getProperty("longitude"));

        // and print the tree
//        printConceptTree(System.out, list.get(0));
//        printGraph(graph);
        //add a Link from a DocUnit/DocDesc to the Ghetto-0
        
       InputStream iosV2 = ClassLoader.getSystemResourceAsStream(SKOS_V2_FILE);
       int origCount = getNodeCount(graph);
        // Before...
       List<VertexProxy> graphState1 = getGraphState(graph);
        ImportLog logv2 = importer.importFile(iosV2, logMessage);
        // After...

       List<VertexProxy> graphState2 = getGraphState(graph);
       GraphDiff diff = diffGraph(graphState1, graphState2);
       diff.printDebug(System.out);
       /*
        * CREATED:
        * null: 3
        * cvocConceptDescription: 7
        * systemEvent: 1
        * 
        * REMOVED:
        * relationship: 4
        * cvocConceptDescription: 4
        */
       assertEquals(origCount + (11-8), getNodeCount(graph));
        printGraph(graph);
        
        Concept ghetto0v2 = manager.getEntity("cvoc1-0", Concept.class);
        assertEquals(ghetto0, ghetto0v2);
        assertEquals("48.6666666667", ghetto0v2.getProperty("latitude"));
        assertEquals("26.5666666667", ghetto0v2.getProperty("longitude"));
    }
}
