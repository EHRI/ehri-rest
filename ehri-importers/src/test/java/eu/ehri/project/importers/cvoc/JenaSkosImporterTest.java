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

import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.cvoc.Concept;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import org.junit.Ignore;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class JenaSkosImporterTest extends AbstractSkosImporterTest {
    @Test
    public void testImportFile1() throws Exception {
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
        ImportLog importLog = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE1), "simple 1");
        assertEquals(1, importLog.getCreated());
    }
    
    @Test @Ignore
    public void testEhriSkos() throws Exception {
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
        ImportLog importLog = importer.setDefaultLang("de")
                .importFile(ClassLoader.getSystemResourceAsStream("cvoc/ehri-skos.rdf"), "ehri-skos");
        assertEquals(877, importLog.getCreated());
        AccessibleEntity concept = importLog.getAction().getFirstSubject();
        assertEquals("deu", manager.cast(concept, Concept.class)
                .getDescriptions().iterator().next().getLanguageOfDescription());
    }

    @Test
    public void testSetDefaultLang() throws Exception {
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
        // Setting a two-letter language code should result in a description
        // being created with the corresponding 3-letter code.
        ImportLog importLog = importer.setDefaultLang("de")
                .importFile(ClassLoader.getSystemResourceAsStream(FILE1), "simple 1");
        assertEquals(1, importLog.getCreated());
        AccessibleEntity concept = importLog.getAction().getFirstSubject();
        assertEquals("deu", manager.cast(concept, Concept.class)
                .getDescriptions().iterator().next().getLanguageOfDescription());
    }

    @Test
    public void testImportFile2() throws Exception {
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary)
            .setFormat("N3");
        ImportLog importLog = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE2), "simple 2");
        assertEquals(1, importLog.getCreated());
    }

    @Test
    public void testImportFile3() throws Exception {
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
        ImportLog importLog1 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE3), "repositories");
        assertEquals(23, importLog1.getCreated());
        ImportLog importLog2 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE3), "repositories");
        assertEquals(23, importLog2.getUnchanged());
    }

    @Test
    public void testImportFile4() throws Exception {
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
        ImportLog importLog1 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE4), "camps");
        assertEquals(8, importLog1.getCreated());
        ImportLog importLog2 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE4), "camps");
        assertEquals(8, importLog2.getUnchanged());
    }

    @Test
    public void testImportFile5() throws Exception {
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
        ImportLog importLog1 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE5), "ghettos");
        assertEquals(2, importLog1.getCreated());
        ImportLog importLog2 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE5), "ghettos");
        assertEquals(2, importLog2.getUnchanged());
    }
}
