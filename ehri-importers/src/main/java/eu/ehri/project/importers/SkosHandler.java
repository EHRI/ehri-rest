/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import eu.ehri.project.models.base.Frame;
import org.xml.sax.SAXException;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.cvoc.Concept;
import java.util.ArrayList;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 *
 * makes use of skos.properties with format: part/of/path/=attribute
 *
 * @author linda
 */
public class SkosHandler extends SaxXmlHandler {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SkosHandler.class);
    Map<String, Class<? extends Frame>> possibleSubnodes;
    Stack<String> prefixStack;

    public SkosHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("skos.properties"));
        prefixStack = new Stack<String>();
        possibleSubnodes = new HashMap<String, Class<? extends Frame>>();
        possibleSubnodes.put("concept", Concept.class);
    }

    protected boolean needToCreateSubNode() {
        return possibleSubnodes.containsKey(getImportantPath(currentPath));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        logger.debug("endElement: " + qName);

        //if a subnode is ended, add it to the super-supergraph
        super.endElement(uri, localName, qName);
        if (needToCreateSubNode()) {
            logger.debug("Subnode needed at depth: " + depth + ", key: " + getImportantPath(currentPath));

            Map<String, Object> currentGraph = currentGraphPath.pop();
            putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
            depth--;
        }

        currentPath.pop();
        logger.debug("currentPath: " + currentPath.toString());

        //TODO: a skos file consists of multiple elements, so we will create multiple Concepts
        if (currentPath.isEmpty()) {
            logger.debug("Empty Path");

//--- begin TEST CODE
// But I don't want the RDF node, only the concepts...
// so we need the subGraphs...
            Map<String, Object> c = currentGraphPath.peek();
            if (c.containsKey("concept")) {
                List<Map<String, Object>> concepts = (List<Map<String, Object>>) c.get("concept");
                logger.debug("#### concepts: " + concepts.size());
                if (!concepts.isEmpty()) {
                    // just test
                    try {
                        importer.importItem(concepts.get(0), depth);
                    } catch (ValidationError e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } else {
                // ignore
                currentGraphPath.pop();
            }
//--- end TEST CODE
/* Original code
             //TODO: add all languagespecific stuff:
             for(String lang : languageMap.keySet())
             System.out.println("lang: " + lang);
             System.out.println("lang size: " + languageMap.size());
             try {
             System.out.println("depth close " + depth + " " + qName);
             //TODO: add any mandatory fields not yet there:
             if (!currentGraphPath.peek().containsKey("objectIdentifier")) {
             putPropertyInCurrentGraph("objectIdentifier", "id");
             }
             if (!currentGraphPath.peek().containsKey("name")) {
             putPropertyInCurrentGraph("name", "QQQ name");
             }
             //importer.importItem(currentGraphPath.pop(), depth);
             importer.importItem(currentGraphPath.pop(), depth);// No parent !!!!!!

             } catch (ValidationError ex) {
             Logger.getLogger(SkosHandler.class.getName()).log(Level.SEVERE, null, ex);
             }
             */
        }
    }

    @Override
    protected boolean needToCreateSubNode(String qName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected List<String> getSchemas() {
        List<String> schemas = new ArrayList<String>();
        return schemas;
    }
}
