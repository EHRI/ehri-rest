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
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * makes use of properties file with format: part/of/path/=attribute
 *
 * @author linda
 */
public abstract class SaxXmlHandler extends DefaultHandler {
        private static final org.slf4j.Logger logger = LoggerFactory
            .getLogger(SaxXmlHandler.class);
    public static final String UNKNOWN = "UNKNOWN_";
    Stack<Map<String, Object>> currentGraphPath;
    Map<String, Map<String, Object>> languageMap;
    PropertiesConfig p;
    Stack<String> currentPath;
    AbstractImporter<Map<String, Object>> importer;
    String languagePrefix;
    int depth = 0;
    boolean inSubnode = false;

//    public SaxXmlHandler(AbstractCVocImporter<Map<String, Object>> importer2, PropertiesConfig properties) {
//        super();
//        this.importer = importer2;
//        currentGraphPath = new Stack<Map<String, Object>>();
//        currentGraphPath.push(new HashMap<String, Object>());
//        p = properties;
//        currentPath = new Stack<String>();
//        languageMap = new HashMap<String, Map<String, Object>>();
//    }
    public SaxXmlHandler(AbstractImporter<Map<String, Object>> importer, PropertiesConfig properties) {
        super();
        logger.error("constructor");
        this.importer=importer;
        currentGraphPath = new Stack<Map<String, Object>>();
        currentGraphPath.push(new HashMap<String, Object>());
        p = properties;
        currentPath = new Stack<String>();
        languageMap = new HashMap<String, Map<String, Object>>();
    }

    protected abstract boolean needToCreateSubNode();

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {

        String lang = languageAttribute(attributes);
        if (lang != null) {
            languagePrefix = lang;
            if (!languageMap.containsKey(languagePrefix)) {
                if(languageMap.isEmpty())
                    currentGraphPath.peek().put("languageCode", languageMap);
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("languageCode", languagePrefix);
                languageMap.put(languagePrefix, m);
            }
        }

        currentPath.push(withoutNamespace(qName));

        if (needToCreateSubNode()) { //a new subgraph should be created
            System.out.println("new subnode: " + depth);
            depth++;
            currentGraphPath.push(new HashMap<String, Object>());
        }

        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
            String attribute = withoutNamespace(attributes.getLocalName(attr));
            if (p.hasAttributeProperty(attribute) && ! p.getAttributeProperty(attribute).equals("languageCode")) {
                putPropertyInCurrentGraph(p.getAttributeProperty(attribute), attributes.getValue(attr));
            }
        }

    }
        @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
            languagePrefix=null;
    }


    private String languageAttribute(Attributes attributes) {
        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
            String attribute = withoutNamespace(attributes.getLocalName(attr));
 // logging here
 //if (p.getAttributeProperty(attribute) != null) {
//	 System.out.println("attribute: " + attribute
//			 	+ ", prop: " + p.getAttributeProperty(attribute) 
//			 	+ ", val: " + attributes.getValue(attr));
// }
            
            if (p.getAttributeProperty(attribute) != null && p.getAttributeProperty(attribute).equals("languageCode")) {
 //System.out.println("Language detected!");
            	return attributes.getValue(attr);
            }
        }
        return null;
    }

    private String withoutNamespace(String qName) {
        String name = qName;
        int colon = qName.indexOf(":");
        if (colon > -1) {
//            assert(qName.substring(0, colon).equals(prefixStack.peek()));
            name = qName.substring(colon + 1);
        }
        return name;
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if (isEmpty(new String(ch, start, length))) {
            return;
        }
        if(languagePrefix == null){
           putPropertyInCurrentGraph(getImportantPath(currentPath), new String(ch, start, length));
        }else{
            putPropertyInGraph(languageMap.get(languagePrefix), getImportantPath(currentPath), new String(ch, start, length));
        }
    }

    /**
     * stores this property value pair in the current DocumentNode if the
     * property already exists, it is added to the value list
     *
     * @param property
     * @param value
     */
    protected void putPropertyInCurrentGraph(String property, String value) {
        putPropertyInGraph(currentGraphPath.peek(), property, value);
    }
    private void putPropertyInGraph(Map<String, Object> c, String property, String value) {
        String valuetrimmed = value.trim();
        if (valuetrimmed.isEmpty()) {
            return;
        }
        System.out.println("putProp: " + property + " " + value);

        Object propertyList;
        if (c.containsKey(property)) {
            propertyList = c.get(property);
            if (propertyList instanceof List) {
                ((List<Object>) propertyList).add(valuetrimmed);
            } else {
                List<Object> o = new ArrayList<Object>();
                o.add(valuetrimmed);
                o.add(c.get(property));
                c.put(property, o);
            }
        } else {
            c.put(property, valuetrimmed);
        }
    }

    
    private boolean isEmpty(String s) {
        return (s.trim()).isEmpty();
    }

    /**
     *
     * @param path
     * @return returns the corresponding value to this path from the properties
     * file. the search is inside out, so if both eadheader/ and ead/eadheader/
     * are specified, it will return the value for the first
     *
     * if this path has no corresponding value in the properties file, it will
     * be return the entire path name, with _ replacing the /
     */
    protected String getImportantPath(Stack<String> path) {
        String all = "";
        for (int i = path.size(); i > 0; i--) {
            all = path.get(i - 1) + "/" + all;
            if (p.getProperty(all) != null) {
                return p.getProperty(all);
            }
        }
        return UNKNOWN + all.replace("/", "_");
    }

    protected void printGraph() {
        String g = "";
        for (String key : currentGraphPath.peek().keySet()) {
            System.out.println(key + ":" + currentGraphPath.peek().get(key));
        }
    }
}
