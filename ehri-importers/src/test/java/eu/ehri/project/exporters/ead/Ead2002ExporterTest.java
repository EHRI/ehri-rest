package eu.ehri.project.exporters.ead;

import com.google.common.io.Resources;
import eu.ehri.project.importers.AbstractImporterTest;
import eu.ehri.project.importers.IcaAtomEadHandler;
import eu.ehri.project.importers.IcaAtomEadImporter;
import eu.ehri.project.importers.managers.ImportManager;
import eu.ehri.project.importers.managers.SaxImportManager;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Repository;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Ead2002ExporterTest extends AbstractImporterTest {

    @Test
    public void testExport1() throws Exception {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        testExport(c1, "eng");
    }

    @Test
    public void testExport2() throws Exception {
        DocumentaryUnit c4 = manager.getFrame("c4", DocumentaryUnit.class);
        testExport(c4, "eng");
    }

    @Test
    public void testImportExport1() throws Exception {
        Repository repo = manager.getFrame("r1", Repository.class);
        testImportExport(repo, "hierarchical-ead.xml", "Ctop level fonds", "eng");
    }

    @Test
    public void testImportExport2() throws Exception {
        Repository repo = manager.getFrame("r1", Repository.class);
        String xml = testImportExport(repo, "comprehensive-ead.xml",
                "Resource (call) |||.Ident (num) |||", "eng");
        //System.out.println(xml);
        Document doc = parseDocument(xml);
        assertXPath(doc, "Testing import/export [ingest]",
                "//ead/eadheader/revisiondesc/change/item/text()");
        assertXPath(doc, "eng",
                "//ead/eadheader/profiledesc/langusage/language/@langcode");
        assertXPath(doc, "NIOD Description",
                "//ead/eadheader/filedesc/publicationstmt/publisher/text()");
        assertXPath(doc, "NIOD Description",
                "//ead/archdesc/did/repository/corpname/text()");
        assertXPath(doc, "Scope and contents note content no label |||",
                "//ead/archdesc/scopecontent/p/text()");
        assertXPath(doc, "Series I",
                "//ead/archdesc/dsc/c01/did/unitid/text()");
        assertXPath(doc, "Folder 3 |||",
                "//ead/archdesc/dsc/c01[2]/c02[6]/did/unitid/text()");
    }

    private String testExport(DocumentaryUnit unit, String lang) throws Exception {
        Ead2002Exporter exporter = new Ead2002Exporter(graph);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exporter.export(unit, baos, lang);
        String xml = baos.toString("UTF-8");
        isValidEad(xml);
        return xml;
    }

    private String testImportExport(Repository repository, String resourceName,
            String topLevelIdentifier, String lang) throws Exception {
        InputStream ios = ClassLoader.getSystemResourceAsStream(resourceName);
        ImportManager importManager = new SaxImportManager(graph, repository, validUser,
                IcaAtomEadImporter.class, IcaAtomEadHandler.class);
        importManager.importFile(ios, "Testing import/export");

        DocumentaryUnit fonds = graph.frame(
                getVertexByIdentifier(graph, topLevelIdentifier), DocumentaryUnit.class);
        Ead2002Exporter exporter = new Ead2002Exporter(graph);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exporter.export(fonds, baos, lang);
        String xml = baos.toString("UTF-8");
        isValidEad(xml);
        return xml;
    }

    private Document parseDocument(String xml) throws Exception {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return documentBuilder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    private void isValidEad(String eadXml) throws IOException, SAXException {
        SchemaFactory factory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(Resources.getResource("ead.xsd"));
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new ByteArrayInputStream(eadXml.getBytes("UTF-8"))));
    }

    private void assertXPath(Document doc, String value, String path) throws Exception {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        XPathExpression expr = xPath.compile(path);
        Object out = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals(value, out);
    }
}