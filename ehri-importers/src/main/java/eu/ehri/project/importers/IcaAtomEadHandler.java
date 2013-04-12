
package eu.ehri.project.importers;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.base.Description;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * makes use of icaatom.properties with format: part/of/path/=attribute
 *
 * @author linda
 */
public class IcaAtomEadHandler extends SaxXmlHandler {
    private static final Logger logger = LoggerFactory
            .getLogger(IcaAtomEadHandler.class);
    List<DocumentaryUnit>[] children;
    // Pattern for EAD nodes that represent a child item
    private Pattern childItemPattern = Pattern.compile("^/*c(?:\\d+)$");

    
    @SuppressWarnings("unchecked")
    public IcaAtomEadHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("icaatom.properties"));
        this.importer = importer;
        currentGraphPath = new Stack<Map<String, Object>>();
        currentGraphPath.push(new HashMap<String, Object>());
        children = new ArrayList[12]; //12 is the deepest ead child levels go
        children[depth] = new ArrayList<DocumentaryUnit>();
        currentPath = new Stack<String>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
//        currentPath.push(qName);
        super.startElement(uri, localName, qName, attributes);

        if (childItemPattern.matcher(qName).matches() || qName.equals("archdesc")) { //a new DocumentaryUnit should be created
//            depth++;
//            System.out.println("=============== startElement: " + qName);
//            currentGraphPath.push(new HashMap<String, Object>());
            children[depth] = new ArrayList<DocumentaryUnit>();
        }
//        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
//            if (p.hasAttributeProperty(attributes.getLocalName(attr))) {
//                putPropertyInCurrentGraph(p.getAttributeProperty(attributes.getLocalName(attr)), attributes.getValue(attr));
//            }
//        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //the child closes, add the new DocUnit to the list, establish some relations
        super.endElement(uri, localName, qName);
        if (childItemPattern.matcher(qName).matches() || qName.equals("archdesc")) {
            Map<String, Object> currentGraph = currentGraphPath.pop();
            try {
                //add any mandatory fields not yet there:
                if (!currentGraph.containsKey(Description.NAME)) {
                    for(String key: currentGraph.keySet()){
                        logger.debug(key + ":" + currentGraph.get(key));
                    }
                    //finding some name for this unit:
                    if(currentGraph.containsKey("title"))
                        currentGraph.put(Description.NAME, currentGraph.get("title"));
                    else{
                        logger.error("IcaAtom node without name field: " );
                        currentGraph.put(Description.NAME, "UNKNOWN title");
                    }
                }
                if (!currentGraph.containsKey(Description.LANGUAGE_CODE)) {
                    currentGraph.put(Description.LANGUAGE_CODE, "en");
                }
                DocumentaryUnit current = (DocumentaryUnit)importer.importItem(currentGraph, depth);
                logger.debug("importer used: " + importer.getClass());
                if (depth > 0) { // if not on root level
                    children[depth - 1].add(current); // add child to parent offspring
                    //set the parent child relationships by hand, as we don't have the parent Documentary unit yet.
                    //only when closing a DocUnit, one can set the relationship to its children, but not its parent, as that has not yet been closed.
                    for (DocumentaryUnit child : children[depth]) {
                        if (child != null) {
                            current.addChild(child);
                            child.setPermissionScope(current);
                        }
                    }
                }

            } catch (ValidationError ex) {
                logger.error(ex.getMessage());
            } finally {
                depth--;
            }
        }
        
        currentPath.pop();
    }

    @Override
    protected boolean needToCreateSubNode(String qName) {
        return childItemPattern.matcher(qName).matches() || qName.equals("archdesc");
    }

    @Override
    protected List<String> getSchemas() {
        List<String> schemas = new ArrayList<String>();
        schemas.add("xlink.xsd");
        schemas.add("ead.xsd");
        return schemas;
    }
}
