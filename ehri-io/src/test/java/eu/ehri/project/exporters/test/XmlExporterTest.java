package eu.ehri.project.exporters.test;

import com.google.common.io.Resources;
import eu.ehri.project.importers.base.AbstractImporterTest;
import org.junit.Before;
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

import static org.junit.Assert.assertEquals;


public class XmlExporterTest extends AbstractImporterTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        helper.setInitializing(false)
                .loadTestData(getClass()
                        .getClassLoader().getResourceAsStream("exportdata.yaml"));
    }

    protected Document parseDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        return documentBuilder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    protected void validatesSchema(String xml, String schemaResourceName) throws IOException, SAXException {
        SchemaFactory factory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(Resources.getResource(schemaResourceName));
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
    }

    protected void assertXPath(Document doc, String value, String path) throws Exception {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        XPathExpression expr = xPath.compile(path);
        Object out = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals(value, out);
    }
}
