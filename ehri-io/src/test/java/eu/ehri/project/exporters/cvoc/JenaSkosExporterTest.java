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

package eu.ehri.project.exporters.cvoc;

import com.google.common.collect.ImmutableMap;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.cvoc.AbstractSkosTest;
import eu.ehri.project.importers.cvoc.JenaSkosImporter;
import eu.ehri.project.importers.cvoc.SkosImporter;
import eu.ehri.project.models.cvoc.Vocabulary;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
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

            SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary)
                    .setFormat(entry.getValue().equalsIgnoreCase("") ? null : entry.getValue())
                    .allowUpdates(true);
            importer
                    .importFile(ClassLoader.getSystemResourceAsStream(entry.getKey()), "test");

            List<VertexProxy> before = getGraphState(graph);
            int edgeCountBefore = getEdgeCount(graph);
            SkosExporter exporter = new JenaSkosExporter(graph, vocabulary)
                    .setFormat(entry.getValue().equalsIgnoreCase("") ? null : entry.getValue());
            OutputStream outputStream = new ByteArrayOutputStream();
            exporter.export(outputStream, "http://www.my.com/#");
            String skos = outputStream.toString();
            //System.out.println("EXPORT: " + skos);
            ImportLog log = importer
                    .setFormat(entry.getValue())
                    .allowUpdates(true)
                    .importFile(new ByteArrayInputStream(skos.getBytes()), "test");
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
        String baseUri = "http://ehri01.dans.knaw.nl/";
        importFile(vocabulary, FILE4);

        for (String format : formats) {
            String skos = exportFile(vocabulary, format, baseUri);
            //System.out.println(skos);
            assertTrue(skos.contains(baseUri));
            assertTrue(skos.contains(baseUri + "989"));
        }
    }

    @Test
    public void testLangCodes() throws Exception {
        importFile(vocabulary, FILE5);
        String skos = exportFile(vocabulary, "RDF/XML",
                "http://data.ehri-project.eu/");
        assertThat(skos, containsString("xml:lang=\"ru-latn\""));
    }

    @Test
    public void testAbsoluteURIs() throws Exception {
        importFile(vocabulary, FILE5);
        String skos = exportFile(vocabulary, "RDF/XML",
                "http://ehri01.dans.knaw.nl/");
        assertThat(skos, containsString("rdf:about=\"http://ehri01.dans.knaw.nl/cvoc2\""));
    }

    private void importFile(Vocabulary vocabulary, String file) throws Exception {
        SkosImporter importer = new JenaSkosImporter(graph, actioner, vocabulary);
        importer.importFile(ClassLoader.getSystemResourceAsStream(file), "test");
    }

    private String exportFile(Vocabulary vocabulary, String format, String baseUri) throws Exception {
        SkosExporter exporter = new JenaSkosExporter(graph, vocabulary)
                .setFormat(format);
        OutputStream outputStream = new ByteArrayOutputStream();

        exporter.export(outputStream, baseUri);
        return outputStream.toString();
    }
}