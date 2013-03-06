/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.SAXException;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.cvoc.Concept;

/**
 *
 * makes use of skos.properties with format: part/of/path/=attribute
 *
 * @author linda
 */
public class SkosHandler extends SaxXmlHandler {

    Map<String, Class<? extends VertexFrame>> possibleSubnodes;
    Stack<String> prefixStack;

    // Note: CVoc specific !
    @SuppressWarnings("unchecked")
    public SkosHandler(AbstractCVocImporter<Map<String, Object>> importer) {
        super(importer, new PropertiesConfig("skos.properties"));
        this.importer = importer;
        currentGraphPath = new Stack<Map<String, Object>>();
        currentGraphPath.push(new HashMap<String, Object>());
        currentPath = new Stack<String>();
        prefixStack = new Stack<String>();
        possibleSubnodes = new HashMap<String, Class<? extends VertexFrame>>();
        possibleSubnodes.put("concept", Concept.class);
    }

    protected boolean needToCreateSubNode() {
        return possibleSubnodes.containsKey(getImportantPath(currentPath));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	System.out.println("endElement: " + qName);
    	
    	//if a subnode is ended, add it to the super-supergraph
        super.endElement(uri, localName, qName);
        if (needToCreateSubNode()) {
        	System.out.println("Subnode needed at depth: " + depth + ", key: " + getImportantPath(currentPath));
        	
            Map<String, Object> currentGraph = currentGraphPath.pop();
            putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
            depth--;
        }

        currentPath.pop();
        System.out.println("currentPath: " + currentPath.toString());        

        //TODO: a skos file consists of multiple elements, so we will create multiple Concepts
        if (currentPath.isEmpty()) {
System.out.println("Empty Path");

//--- begin TEST CODE
// But I don't want the RDF node, only the concepts...
// so we need the subGraphs...
Map<String, Object> c = currentGraphPath.peek();
if (c.containsKey("concept")) {
	List<Map<String, Object>> concepts = (List<Map<String, Object>>) c.get("concept");
System.out.println("#### concepts: " + concepts.size());
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

    @Override
    protected boolean needToCreateSubNode(String qName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
