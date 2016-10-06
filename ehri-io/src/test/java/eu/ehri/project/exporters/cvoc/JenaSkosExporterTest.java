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

package eu.ehri.project.exporters.cvoc;

import com.google.common.collect.ImmutableMap;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.cvoc.AbstractSkosTest;
import eu.ehri.project.importers.cvoc.JenaSkosImporter;
import eu.ehri.project.importers.cvoc.SkosImporter;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class JenaSkosExporterTest extends AbstractSkosTest {

    @Test
    public void testImportExportRoundtrip() throws Exception {
        Map<String, String> files = ImmutableMap.of(
                FILE1, "RDF/XML",
                FILE2, "N3",
                FILE3, "RDF/XML",
                FILE4, "RDF/XML",
                FILE5, "RDF/XML"
        );
        for (Map.Entry<String, String> entry : files.entrySet()) {
            SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
            importer.setFormat(entry.getValue().equalsIgnoreCase("") ? null : entry.getValue())
                    .importFile(ClassLoader.getSystemResourceAsStream(entry.getKey()), "test");

            List<VertexProxy> before = getGraphState(graph);
            int edgeCountBefore = getEdgeCount(graph);
            SkosExporter exporter = new JenaSkosExporter(graph, vocabulary)
                    .setFormat(entry.getValue().equalsIgnoreCase("") ? null : entry.getValue());
            OutputStream outputStream = new ByteArrayOutputStream();
            exporter.export(outputStream, null);
            String skos = outputStream.toString();
            ImportLog log = importer.setFormat(entry.getValue())
                    .importFile(new ByteArrayInputStream(skos.getBytes()), "test");
            log.printReport();
            List<VertexProxy> after = getGraphState(graph);
            assertTrue(log.getUnchanged() > 0);
            assertEquals(0, log.getChanged());
            assertEquals(0, log.getCreated());
            assertEquals(0, log.getErrored());
            GraphDiff graphDiff = diffGraph(before, after);
            assertTrue(graphDiff.added.isEmpty());
            assertTrue(graphDiff.removed.isEmpty());
            int edgeCountAfter = getEdgeCount(graph);
            assertEquals(edgeCountBefore, edgeCountAfter);
        }
    }

    @Test
    public void testExport() throws Exception {
        String[] formats = {"TTL", "RDF/XML", "N3"};
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
        String baseUri = "http://example.com#";
        importer.importFile(ClassLoader.getSystemResourceAsStream(FILE4), "test");

        for (String format : formats) {
            SkosExporter exporter = new JenaSkosExporter(graph, vocabulary)
                    .setFormat(format);
            OutputStream outputStream = new ByteArrayOutputStream();

            exporter.export(outputStream, baseUri);
            String skos = outputStream.toString();
            //System.out.println(skos);
            assertTrue(skos.contains(baseUri));
            assertTrue(skos.contains(baseUri + "989"));
        }
    }
}