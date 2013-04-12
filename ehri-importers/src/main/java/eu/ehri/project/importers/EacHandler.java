/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.base.Description;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.ehri.project.models.base.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * makes use of eac.properties with format: part/of/path/=attribute
 *
 * @author linda
 */
public class EacHandler extends EaHandler {

    Map<String, Class<? extends Frame>> possibleSubnodes;
    private static final Logger logger = LoggerFactory.getLogger(EacHandler.class);

    public EacHandler(AbstractImporter<Map<String, Object>> importer) {

        super(importer, new XmlImportProperties("eac.properties"));
        possibleSubnodes = new HashMap<String, Class<? extends Frame>>();

        possibleSubnodes.put("maintenanceEvent", MaintenanceEvent.class);
        possibleSubnodes.put("relation", Annotation.class);
    }

    @Override
    protected boolean needToCreateSubNode(String key) {
        return possibleSubnodes.containsKey(getImportantPath(currentPath));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //if a subnode is ended, add it to the super-supergraph
        super.endElement(uri, localName, qName);
        if (needToCreateSubNode(getImportantPath(currentPath))) {
            Map<String, Object> currentGraph = currentGraphPath.pop();
            putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
            depth--;
        }

        currentPath.pop();

        //an EAC file consists of only 1 element, so if we're back at the root, we're done
        if (currentPath.isEmpty()) {
            try {
                logger.debug("depth close " + depth + " " + qName);
                //TODO: add any mandatory fields not yet there:
                if (!currentGraphPath.peek().containsKey("objectIdentifier")) {
                    putPropertyInCurrentGraph("objectIdentifier", "id");
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

    @Override
    protected List<String> getSchemas() {
        List<String> schemas = new ArrayList<String>();
        schemas.add("eac.xsd");
        return schemas;
    }
}
