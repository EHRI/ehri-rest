/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * @author linda
 */
public class Wp2EadHandler extends IcaAtomEadHandler {

    private static final Logger logger = LoggerFactory.getLogger(Wp2EadHandler.class);

    public Wp2EadHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("wp2ead.properties"));
    }

    
  @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
            if (properties.hasAttributeProperty(attributes.getQName(attr))) {
                putPropertyInCurrentGraph(properties.getAttributeProperty(attributes.getQName(attr)),
                        attributes.getValue(attr));
            }
        }
    }
    
}
