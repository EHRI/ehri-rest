package eu.ehri.project.test;

import com.google.common.io.Resources;
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

public class XmlTestHelpers {
    private static final XPathFactory factory = XPathFactory.newInstance();
    private static final XPath xPath = factory.newXPath();

    public static Document parseDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        return documentBuilder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    public static void validatesSchema(String xml, String schemaResourceName) throws IOException, SAXException {
        SchemaFactory factory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setResourceResolver(new LocalResourceResolver());

        Schema schema = factory.newSchema(Resources.getResource(schemaResourceName));
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
    }

    public static void assertXPath(Document doc, String value, String path) throws Exception {
        XPathExpression expr = xPath.compile(path);
        Object out = expr.evaluate(doc, XPathConstants.STRING);
        assertEquals(value, out);
    }

    public static Object xPath(Document doc, String path) throws Exception {
        XPath xPath = factory.newXPath();
        XPathExpression expr = xPath.compile(path);
        return expr.evaluate(doc, XPathConstants.STRING);
    }
}
