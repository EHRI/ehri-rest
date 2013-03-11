package eu.ehri.project.test;

import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.Serializer;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for Bundle data conversion functions.
 */
public class DataConverterTest extends AbstractFixtureTest {
private static final Logger logger = LoggerFactory.getLogger(DataConverterTest.class);
    @Test
    public void testBundleToXml() throws Exception {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        Bundle bundle = new Serializer(graph).vertexFrameToBundle(c1)
                .withDataValue("testarray", new String[] { "one", "two", "three" });

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
        logger.debug(bundle.toXmlString());
    }
}
