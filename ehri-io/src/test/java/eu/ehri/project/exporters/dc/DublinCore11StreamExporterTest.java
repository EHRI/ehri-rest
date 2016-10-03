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

package eu.ehri.project.exporters.dc;

import eu.ehri.project.exporters.test.XmlExporterTest;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class DublinCore11StreamExporterTest extends XmlExporterTest {

    @Test
    public void testExport1() throws Exception {
        HistoricalAgent agent = manager.getEntity("a1", HistoricalAgent.class);
        testExport(agent, "eng");
    }

    @Test
    public void testExportWithComprehensiveFixture() throws Exception {
        DocumentaryUnit test = manager.getEntity("nl-000001-1", DocumentaryUnit.class);
        String xml = testExport(test, "eng");
        Document doc = parseDocument(xml);
        assertXPath(doc, "1", "//dc/identifier");
        assertXPath(doc, "fonds", "//dc/type");
        assertXPath(doc, "Institution Example", "//dc/publisher");
        assertXPath(doc, "1939-01-01 - 1945-01-01", "//dc/coverage");
        assertXPath(doc, "Example text", "//dc/description");
        assertXPath(doc, "Example Person 1", "//dc/relation");
        assertXPath(doc, "Example Subject 1", "//dc/subject");
    }

    private String testExport(Described item, String lang) throws Exception {
        DublinCoreExporter exporter = new DublinCore11StreamExporter(api(validUser));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exporter.export(item, baos, lang);
        String xml = baos.toString("UTF-8");
        //System.out.println(xml);
        isValidDc(xml);
        return xml;
    }

    private void isValidDc(String dcXml) throws IOException, SAXException {
        validatesSchema(dcXml, "oai_dc.xsd");
    }
}