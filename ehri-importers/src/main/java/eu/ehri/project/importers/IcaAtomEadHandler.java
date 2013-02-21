
package eu.ehri.project.importers;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
    private static final org.slf4j.Logger logger = LoggerFactory
            .getLogger(IcaAtomEadHandler.class);
    List<DocumentaryUnit>[] children;
    // Pattern for EAD nodes that represent a child item
    private Pattern childItemPattern = Pattern.compile("^/*c(?:\\d+)$");

    
    @SuppressWarnings("unchecked")
    public IcaAtomEadHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new PropertiesConfig("icaatom.properties"));
        logger.error("constructor");
        this.importer = importer;
        currentGraphPath = new Stack<Map<String, Object>>();
        currentGraphPath.push(new HashMap<String, Object>());
        children = new ArrayList[12]; //12 is the deepest ead child levels go
        children[depth] = new ArrayList<DocumentaryUnit>();
        currentPath = new Stack<String>();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        logger.error("start: " + qName);
        
        currentPath.push(qName);

        if (childItemPattern.matcher(qName).matches()) { //a new DocumentaryUnit should be created
            depth++;
            currentGraphPath.push(new HashMap<String, Object>());
            children[depth] = new ArrayList<DocumentaryUnit>();
        }
        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
            if (p.hasAttributeProperty(attributes.getLocalName(attr))) {
                putPropertyInCurrentGraph(p.getAttributeProperty(attributes.getLocalName(attr)), attributes.getValue(attr));
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //the child closes, add the new DocUnit to the list, establish some relations
        if (childItemPattern.matcher(qName).matches()) {
            Map<String, Object> currentGraph = currentGraphPath.pop();
            try {
                //add any mandatory fields not yet there:
                if (!currentGraph.containsKey("name")) {
                    currentGraph.put("name", currentGraph.get("title"));
                }
                if (!currentGraph.containsKey("languageCode")) {
                    currentGraph.put("languageCode", "en");
                }
                DocumentaryUnit current = (DocumentaryUnit)importer.importItem(currentGraph, depth);
                if (depth > 0) { // if not on root level
                    children[depth - 1].add(current); // add child to parent offspring
                    //set the parent child relationships by hand, as we don't have the parent Documentary unit yet.
                    //only when closing a DocUnit, one can set the relationship to its children, but not its parent, as that has not yet been closed.
                    for (DocumentaryUnit child : children[depth]) {
                        if (child != null) {
                            current.addChild(child);
                        }
                    }
                }
                depth--;
            } catch (ValidationError ex) {
                Logger.getLogger(IcaAtomEadHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        currentPath.pop();
    }

    @Override
    protected boolean needToCreateSubNode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
