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

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.views.Query;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class Wp2PlacesImporterTest extends AbstractImporterTest {
    protected final String SKOS_FILE = "cvoc/wp2_skos_places.xml";

    @Test
    public void testImportItemsT() throws Exception {
        final String logMessage = "Importing the WP2 places file";

        Vocabulary vocabulary = manager.getFrame("cvoc1", Vocabulary.class);

        int count = getNodeCount(graph);
        int voccount = toList(vocabulary.getConcepts()).size();
        InputStream ios = ClassLoader.getSystemResourceAsStream(SKOS_FILE);
        SkosImporter importer = SkosImporterFactory.newSkosImporter(graph, validUser, vocabulary);
        importer.setTolerant(true);
        ImportLog log = importer.importFile(ios, logMessage);

        printGraph(graph);
        /*  How many new nodes will have been created? We should have
         * 2 more Concepts
         * 4 more ConceptDescription
     	 * 3 more import Event links (2 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count + 10, getNodeCount(graph));
        assertEquals(2, log.getCreated());
        assertEquals(voccount + 2, toList(vocabulary.getConcepts()).size());

        // get a top concept
        String skosConceptId = "PLACE.ČSÚ.544256";
        Query<Concept> query = new Query<Concept>(graph, Concept.class);

        // Query for document identifier.
        List<Concept> list = toList(query.setLimit(1).page(
                Ontology.IDENTIFIER_KEY, skosConceptId, validUser));

        Concept location = list.get(0);
        //  <geo:lat>52.43333333333333</geo:lat>
        //	<geo:long>20.716666666666665</geo:long>
        Iterable<Description> ghetto0desc = location.getDescriptions();
        for (Description d : ghetto0desc) {
            assertEquals("48.9756578", d.asVertex().getProperty("latitude"));
            assertEquals("14.480255", d.asVertex().getProperty("longitude"));
        }

        // and print the tree
        printConceptTree(System.out, list.get(0));
    }
}
