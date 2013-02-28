/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.frames.VertexFrame;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.MaintenanceEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
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
public class EacHandler extends SaxXmlHandler {

    Map<String, Class<? extends VertexFrame>> possibleSubnodes;
    private static final Logger logger = LoggerFactory.getLogger(EacHandler.class);

    @SuppressWarnings("unchecked")
    public EacHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new PropertiesConfig("eac.properties"));
        this.importer = importer;
        currentGraphPath = new Stack<Map<String, Object>>();
        currentGraphPath.push(new HashMap<String, Object>());
        currentPath = new Stack<String>();
        possibleSubnodes = new HashMap<String, Class<? extends VertexFrame>>();
        possibleSubnodes.put("maintenanceEvent", MaintenanceEvent.class);
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
                logger.error("depth close " + depth + " " + qName);
                //TODO: add any mandatory fields not yet there:
                if (!currentGraphPath.peek().containsKey("objectIdentifier")) {
                    putPropertyInCurrentGraph("objectIdentifier", "id");
                }
                if (!currentGraphPath.peek().containsKey("title")) {
                    putPropertyInCurrentGraph("title", "title");
                }
                if (!currentGraphPath.peek().containsKey("name")) {
                    putPropertyInCurrentGraph("name", "QQQ name");
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

    @SuppressWarnings("unchecked")
    private void putSubGraphInCurrentGraph(String key, Map<String, Object> subgraph) {
        Map<String, Object> c = currentGraphPath.peek();
        if (c.containsKey(key)) {
            ((List<Map<String, Object>>) c.get(key)).add(subgraph);
        } else {
            List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
            l.add(subgraph);
            c.put(key, l);
        }
    }

}

