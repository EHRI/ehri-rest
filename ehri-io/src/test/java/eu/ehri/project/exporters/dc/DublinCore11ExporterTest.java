package eu.ehri.project.exporters.dc;

import eu.ehri.project.exporters.test.XmlExporterTest;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.base.Described;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DublinCore11ExporterTest extends XmlExporterTest {

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
        assertXPath(doc, "Example text", "//dc/description");
        assertXPath(doc, "Example Person 1", "//dc/relation");
        assertXPath(doc, "Example Subject 1", "//dc/subject");
    }

    private String testExport(Described item, String lang) throws Exception {
        DublinCoreExporter exporter = new DublinCore11Exporter(graph, api(validUser));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exporter.export(item, baos, lang);
        String xml = baos.toString("UTF-8");
        isValidDc(xml);
        return xml;
    }

    private void isValidDc(String dcXml) throws IOException, SAXException {
        validatesSchema(dcXml, "oai_dc.xsd");
    }
}