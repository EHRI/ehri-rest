/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.base.Description;
import java.util.HashMap;
import java.util.Map;

import eu.ehri.project.models.base.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 *
 * to be used in conjunction with EagImporter
 *
 * @author linda
 */
public class EagHandler extends SaxXmlHandler {

    Map<String, Class<? extends Frame>> possibleSubnodes;
    private static final Logger logger = LoggerFactory.getLogger(EagHandler.class);

    public EagHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new PropertiesConfig("eag.properties"));
        possibleSubnodes = new HashMap<String, Class<? extends Frame>>();
        possibleSubnodes.put("maintenanceEvent", MaintenanceEvent.class);
    }

    @Override
    protected boolean needToCreateSubNode(String qName) {
        return possibleSubnodes.containsKey(getImportantPath(currentPath));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //if a subnode is ended, add it to the super-supergraph
        super.endElement(uri, localName, qName);

        if (needToCreateSubNode(qName)) {
            logger.debug("endElement: " + qName);

            logger.debug("just before popping: " + depth + "-" + getImportantPath(currentPath) + "-" + qName);
            Map<String, Object> currentGraph = currentGraphPath.pop();
            putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
//            currentGraphPath.pop();
            depth--;
        }
        
        currentPath.pop();
        //an EAG file consists of only 1 element, so if we're back at the root, we're done
        if (currentPath.isEmpty()) {
            try {
                logger.debug("depth close " + depth + " " + qName);
                //TODO: add any mandatory fields not yet there:
                if (!currentGraphPath.peek().containsKey("objectIdentifier")) {
                    putPropertyInCurrentGraph("objectIdentifier", "id");
                }
                if (!currentGraphPath.peek().containsKey("typeOfEntity")) {
                    putPropertyInCurrentGraph("typeOfEntity", "organisation");
                }
                if (!currentGraphPath.peek().containsKey(Description.NAME)) {
                    logger.debug("no " + Description.NAME + " found");
                    putPropertyInCurrentGraph(Description.NAME, "title");
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
