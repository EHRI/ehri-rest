package eu.ehri.project.test;

import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.Serializer;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Test for Bundle data conversion functions.
 */
public class DataConverterTest extends AbstractFixtureTest {

    @Test
    public void testBundleToXml() throws Exception {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        Bundle bundle = new Serializer(graph).vertexFrameToBundle(c1);

        Document document = bundle.toXml();
        assertTrue(document.hasChildNodes());
        NodeList root = document.getChildNodes();
        assertEquals(1, root.getLength());
        Node rootItem = root.item(0);
        assertNotNull(rootItem.getAttributes().getNamedItem(Bundle.ID_KEY));
        assertNotNull(rootItem.getAttributes().getNamedItem(Bundle.TYPE_KEY));
        assertEquals("c1", rootItem.getAttributes().getNamedItem(Bundle.ID_KEY).getNodeValue());
        assertEquals(EntityClass.DOCUMENTARY_UNIT.getName(),
                rootItem.getAttributes().getNamedItem(Bundle.TYPE_KEY).getNodeValue());
        assertTrue(rootItem.hasChildNodes());
        // TODO: Check properties and relationships are serialized properly
        System.out.println(bundle.toXmlString());
    }
}
