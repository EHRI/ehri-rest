/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;

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
        currentPath.pop();

        //an Niod Europeana file consists of only 1 element, so if we're back at the root, we're done
        if (currentPath.isEmpty()) {
            try {
                logger.debug("depth close " + depth + " " + qName);
                //add any mandatory fields not yet there:
                if (!currentGraphPath.peek().containsKey("objectIdentifier")) {
                    putPropertyInCurrentGraph("objectIdentifier", "id");
                    logger.info("no objectIdentifier found");
                }
                if (!currentGraphPath.peek().containsKey("descriptionIdentifier")) {
                    putPropertyInCurrentGraph("descriptionIdentifier", currentGraphPath.peek().get("objectIdentifier") + "#desc");
                }

                //name can have only 1 value, others are otherFormsOfName
                if (currentGraphPath.peek().containsKey(Ontology.NAME_KEY)) {
                    String name = chooseName(currentGraphPath.peek().get(Ontology.NAME_KEY));
                    overwritePropertyInCurrentGraph(Ontology.NAME_KEY, name);

                }
                if (!currentGraphPath.peek().containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
                    putPropertyInCurrentGraph(Ontology.LANGUAGE_OF_DESCRIPTION, "en");
                    logger.debug("no " + Ontology.LANGUAGE_OF_DESCRIPTION + " found, default language inserted: " + currentGraphPath.peek().get(Ontology.LANGUAGE_OF_DESCRIPTION));
                }
                importer.importItem(currentGraphPath.pop(), Lists.<String>newArrayList());

            } catch (ValidationError ex) {
                logger.error(ex.getMessage());
            }
        }
    }
}
