/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Description;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author linda
 */
public class NiodEuropeanaHandler extends EaHandler {
    private static final Logger logger = LoggerFactory.getLogger(NiodEuropeanaHandler.class);
    
    public NiodEuropeanaHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("niod.properties"));
    }

    @Override
    protected List<String> getSchemas() {
        return new ArrayList<String>();
    }

    @Override
    protected boolean needToCreateSubNode(String qName) {
        return false;
    }
    
     @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //the child closes, add the new DocUnit to the list, establish some relations
        super.endElement(uri, localName, qName);
        logger.debug(currentPath.size() + ": " +currentPath.peek());
        currentPath.pop();

        //an Niod Europeana file consists of only 1 element, so if we're back at the root, we're done
        if (currentPath.isEmpty()) {
            try {
                logger.debug("depth close " + depth + " " + qName);
                //TODO: add any mandatory fields not yet there:
                if (!currentGraphPath.peek().containsKey("objectIdentifier")) {
                    putPropertyInCurrentGraph("objectIdentifier", "id");
                }
                if (!currentGraphPath.peek().containsKey("descriptionIdentifier")) {
                    putPropertyInCurrentGraph("descriptionIdentifier", currentGraphPath.peek().get("objectIdentifier") + "#desc");
                }

                //TODO: name can have only 1 value, others are otherFormsOfName
                if (currentGraphPath.peek().containsKey(Description.NAME)) {
                    String name = chooseName(currentGraphPath.peek().get(Description.NAME));
                    overwritePropertyInCurrentGraph(Description.NAME, name);

                }
                if (!currentGraphPath.peek().containsKey(Description.LANGUAGE_CODE)) {
                    logger.debug("no " + Description.LANGUAGE_CODE + " found");
                    putPropertyInCurrentGraph(Description.LANGUAGE_CODE, "en");
                }
                importer.importItem(currentGraphPath.pop(), depth);

            } catch (ValidationError ex) {
                logger.error(ex.getMessage());
            }
        }
        
    }
    
}
