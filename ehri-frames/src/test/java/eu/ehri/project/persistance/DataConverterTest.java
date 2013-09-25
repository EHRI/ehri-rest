package eu.ehri.project.persistance;

import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.Assert.*;

/**
 * Test for Bundle data conversion functions.
 */
public class DataConverterTest extends AbstractFixtureTest {

    @Test
    public void testBundleToXml() throws Exception {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        Bundle bundle = new Serializer(graph).vertexFrameToBundle(c1)
                .withDataValue("testarray", new String[] { "one", "two", "three" })
                .withDataValue("itemWithLt", "I should be escape because of: <>");

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
