/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.frames.VertexFrame;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.MaintenanceEvent;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 *
 * to be used in conjunction with EacImporter (N.B.: eaC)
 *
 * @author linda
 */
public class EagHandler extends SaxXmlHandler {

    Map<String, Class<? extends VertexFrame>> possibleSubnodes;
    private static final Logger logger = LoggerFactory.getLogger(EagHandler.class);

    public EagHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new PropertiesConfig("eag.properties"));
        possibleSubnodes = new HashMap<String, Class<? extends VertexFrame>>();
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
            logger.error("endElement: " + qName);

            logger.error("just before popping: " + depth + "-" + getImportantPath(currentPath) + "-" + qName);
            Map<String, Object> currentGraph = currentGraphPath.pop();
            putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
//            currentGraphPath.pop();
            depth--;
        }
        
        currentPath.pop();
        //an EAG file consists of only 1 element, so if we're back at the root, we're done
        if (currentPath.isEmpty()) {
            try {
                logger.error("depth close " + depth + " " + qName);
                //TODO: add any mandatory fields not yet there:
                if (!currentGraphPath.peek().containsKey("objectIdentifier")) {
                    putPropertyInCurrentGraph("objectIdentifier", "id");
                }
                if (!currentGraphPath.peek().containsKey("typeOfEntity")) {
                    putPropertyInCurrentGraph("typeOfEntity", "organisation");
                }
                if (!currentGraphPath.peek().containsKey("title")) {
                    putPropertyInCurrentGraph("title", "title");
                }
                if (!currentGraphPath.peek().containsKey("name")) {
                    putPropertyInCurrentGraph("name", currentGraphPath.peek().get("title").toString());
                }
                if (!currentGraphPath.peek().containsKey("languageCode")) {
                    putPropertyInCurrentGraph("languageCode", "en");
                }
                importer.importItem(currentGraphPath.pop(), depth);

            } catch (ValidationError ex) {
                logger.error(ex.getMessage());
            }
        }
    }
}
