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

package eu.ehri.project.persistence;

import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
