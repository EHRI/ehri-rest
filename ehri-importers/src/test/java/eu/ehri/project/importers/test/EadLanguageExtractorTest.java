package eu.ehri.project.importers.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import eu.ehri.project.importers.ead.EadLanguageExtractor;

public class EadLanguageExtractorTest {

    protected static final String SINGLE_EAD = "single-ead.xml";

    // These values depend on the single-ead.xml contents.
    protected final String LANGUAGE_OF_DESCRIPTION = "en";
    protected final String[] LANGUAGES_OF_MATERIAL = { "en", // English
            "fr", // French
            "de", // German
            "iw", // Hebrew
            "ro", // Romanian
            "ji" // Yiddish
    };

    private static Document doc;
    private static Node data;
    private static XPath xpath;

    @BeforeClass
    public static void setUpClass() throws ParserConfigurationException,
            SAXException, IOException, XPathExpressionException {
        // XML parsing boilerplate...
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream ios = ClassLoader.getSystemResourceAsStream(SINGLE_EAD);
        try {
            doc = builder.parse(ios);
        } finally {
            ios.close();
        }
        xpath = XPathFactory.newInstance().newXPath();
        data = (Node) xpath.compile("//archdesc").evaluate(doc,
                XPathConstants.NODE);
    }

    @Test
    public void testLanguageOfDescription() throws Exception {
        EadLanguageExtractor ex = new EadLanguageExtractor(xpath, doc);
        assertEquals(LANGUAGE_OF_DESCRIPTION, ex.getLanguageOfDescription(data));
    }

    @Test
    public void testLanguagesOfMaterials() throws Exception {
        EadLanguageExtractor ex = new EadLanguageExtractor(xpath, doc);
        List<String> langs = ex.getLanguagesOfMaterial(data);
        assertArrayEquals(LANGUAGES_OF_MATERIAL,
                langs.toArray(new String[langs.size()]));
    }
}
