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
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class XmlSkosImporterTest extends AbstractSkosTest {
    @Ignore("XML SKOS importer fails for simple file")
    @Test
    public void testImportFile1() throws Exception {
        SkosImporter importer = new XmlSkosImporter(graph, actioner, vocabulary);
        ImportLog importLog = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE1), "simple 1");
        assertEquals(1, importLog.getCreated());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImportFile2() throws Exception {
        SkosImporter importer = new XmlSkosImporter(graph, actioner, vocabulary);
        importer.setFormat("N3");
        ImportLog importLog = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE2), "simple 2");
        assertEquals(1, importLog.getCreated());
    }

    @Ignore("XML SKOS importer fails for repositories file")
    @Test
    public void testImportFile3() throws Exception {
        SkosImporter importer = new XmlSkosImporter(graph, actioner, vocabulary);
        ImportLog importLog1 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE3), "repositories");
        assertEquals(23, importLog1.getCreated());
        ImportLog importLog2 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE3), "repositories");
        assertEquals(23, importLog2.getUnchanged());
    }

    @Test
    public void testImportFile4() throws Exception {
        SkosImporter importer = new XmlSkosImporter(graph, actioner, vocabulary);
        ImportLog importLog1 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE4), "camps");
        assertEquals(8, importLog1.getCreated());
        ImportLog importLog2 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE4), "camps");
        assertEquals(8, importLog2.getUnchanged());
//        printGraph(graph);
    }

    @Test
    public void testImportFile5() throws Exception {
        SkosImporter importer = new XmlSkosImporter(graph, actioner, vocabulary);
        ImportLog importLog1 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE5), "ghettos");
        assertEquals(2, importLog1.getCreated());
        ImportLog importLog2 = importer
                .importFile(ClassLoader.getSystemResourceAsStream(FILE5), "ghettos");
        assertEquals(2, importLog2.getUnchanged());
    }
}
