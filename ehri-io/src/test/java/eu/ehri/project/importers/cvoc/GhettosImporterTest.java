/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class GhettosImporterTest extends AbstractImporterTest {
    protected final String SKOS_FILE = "cvoc/ghettos.rdf";

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
        //printGraph(graph);

        /*  How many new nodes will have been created? We should have
         * 2 more Concepts
         * 5 more ConceptDescription
         * 5 UndeterminedRelationships
     	 * 3 more import Event links (2 for every Unit, 1 for the User)
         * 1 more import Event
         */
        assertEquals(count + 16, getNodeCount(graph));
        assertEquals(2, log.getCreated());
        assertEquals(voccount + 2, toList(vocabulary.getConcepts()).size());

        // get a top concept
        String skosConceptId = "0";
        QueryApi query = api(validUser).query();

        // Query for document identifier.
        List<Concept> list = toList(query.withLimit(1).page(
                Ontology.IDENTIFIER_KEY, skosConceptId, Concept.class));

        Concept ghetto0 = list.get(0);
        //  <geo:lat>52.43333333333333</geo:lat>
        //	<geo:long>20.716666666666665</geo:long>
        assertEquals("52.43333333333333", ghetto0.getProperty("latitude"));
        assertEquals("20.716666666666665", ghetto0.getProperty("longitude"));

        // and print the tree
        printConceptTree(System.out, list.get(0));
        printGraph(graph);
    }
}
