/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * makes use of properties file with format: 
 * 
 * path/within/xml/=node/property
 *
 * if no <node> is given, it is the default logical-unit or unit-description of this property file. 
 * with eac.properties this would be an HistoricalAgent with an HistoricalAgentDescription
 * if there is a <node> given, it will translate to another graph node, like Address.
 *
 * lines starting with '@' give the attributes:
 * @attribute=tmpname
 * path/within/xml/@tmpname=node/property
 *
 * all tags not included in the properties file that have a  nodevalue will be put in a unknownproperties node, 
 * with an edge to the unit-description.
 *
 * @author linda
 */
public abstract class SaxXmlHandler extends DefaultHandler {

    private static final Logger logger = LoggerFactory.getLogger(SaxXmlHandler.class);
    public static final String UNKNOWN = "UNKNOWN_";
    Stack<Map<String, Object>> currentGraphPath;
    Map<String, Map<String, Object>> languageMap;
    XmlImportProperties p;
    Stack<String> currentPath;
    AbstractImporter<Map<String, Object>> importer;
    String languagePrefix;
    int depth = 0;
    boolean inSubnode = false;
    private Stack<String> currentText ;
    
    protected abstract List<String> getSchemas();

    public SaxXmlHandler(AbstractImporter<Map<String, Object>> importer, XmlImportProperties properties) {
        super();
        this.importer = importer;
        currentGraphPath = new Stack<Map<String, Object>>();
        currentGraphPath.push(new HashMap<String, Object>());
        p = properties;
        currentPath = new Stack<String>();
        languageMap = new HashMap<String, Map<String, Object>>();
        currentText = new Stack<String>();
    }

    protected abstract boolean needToCreateSubNode(String qName);

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        currentText.push("");
        String lang = languageAttribute(attributes);
        if (lang != null) {
            languagePrefix = lang;
            if (!languageMap.containsKey(languagePrefix)) {
                if (languageMap.isEmpty()) {
                    currentGraphPath.peek().put("languageCode", languageMap);
                }
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("languageCode", languagePrefix);
                languageMap.put(languagePrefix, m);
            }
        }

        currentPath.push(withoutNamespace(qName));
        if (needToCreateSubNode(qName)) { //a new subgraph should be created
            depth++;
            System.out.println("Pushing depth... " + depth);
            currentGraphPath.push(new HashMap<String, Object>());
        }

        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
            String attribute = withoutNamespace(attributes.getLocalName(attr));
            if (p.hasAttributeProperty(attribute) && !p.getAttributeProperty(attribute).equals("languageCode")) {
                putPropertyInCurrentGraph(getImportantPath(currentPath, "@"+p.getAttributeProperty(attribute)), attributes.getValue(attr));
            }
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        languagePrefix = null;
        if (languagePrefix == null) {
            putPropertyInCurrentGraph(getImportantPath(currentPath), currentText.pop());
        } else {
            putPropertyInGraph(languageMap.get(languagePrefix), getImportantPath(currentPath), currentText.pop());
        }


    }

    @SuppressWarnings("unchecked")
    protected void putSubGraphInCurrentGraph(String key, Map<String, Object> subgraph) {
        Map<String, Object> c = currentGraphPath.peek();
//        for(String subkey : subgraph.keySet()){
//            logger.debug(subkey + ":" + subgraph.get(key));
//        }
        if (c.containsKey(key)) {
            ((List<Map<String, Object>>) c.get(key)).add(subgraph);
        } else {
            List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
            l.add(subgraph);
            c.put(key, l);
        }
    }

    private String languageAttribute(Attributes attributes) {
        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
            String attribute = withoutNamespace(attributes.getLocalName(attr));
            
            if (p.getAttributeProperty(attribute) != null && p.getAttributeProperty(attribute).equals("languageCode")) {
                logger.debug("Language detected!");
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
        String trimmed = new String(ch, start, length).trim().replaceAll("\\s+", " ");
        currentText.push(currentText.pop()+trimmed);
//        logger.debug(currentPath.peek() + ": "+currentText.peek() + " (" + getImportantPath(currentPath)+")");
    }

    /**
     * stores this property value pair in the current DocumentNode if the property already exists, it is added to the
     * value list
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
        logger.debug("putProp: " + property + " " + value);

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
    protected void overwritePropertyInCurrentGraph(String property, Object value){
        overwritePropertyInCurrentGraph(currentGraphPath.peek(), property, value);
    }
    private void overwritePropertyInCurrentGraph(Map<String, Object> c,String property, Object value){
        logger.debug("overwriteProp: " + property + " " + value);
        c.put(property, value);
    }

    private boolean isEmpty(String s) {
        return (s.trim()).isEmpty();
    }

    /**
     *
     * @param path
     * @return returns the corresponding value to this path from the properties file. the search is inside out, so if
     * both eadheader/ and ead/eadheader/ are specified, it will return the value for the first
     *
     * if this path has no corresponding value in the properties file, it will be return the entire path name, with _
     * replacing the /
     */
    protected String getImportantPath(Stack<String> path) {
        return getImportantPath(path, "");
    }
 /**
     *
     * @param path
     * @return returns the corresponding value to this path from the properties file. the search is inside out, so if
     * both eadheader/ and ead/eadheader/ are specified, it will return the value for the first
     *
     * if this path has no corresponding value in the properties file, it will be return the entire path name, with _
     * replacing the /
     */
    private String getImportantPath(Stack<String> path, String attribute) {
        String all = "";
        for (int i = path.size(); i > 0; i--) {
            all = path.get(i - 1) + "/" + all;
            if (p.getProperty(all+attribute) != null) {
                return p.getProperty(all+attribute);
            }
        }
        return UNKNOWN + all.replace("/", "_");
    }
    protected void printGraph() {
        for (String key : currentGraphPath.peek().keySet()) {
            System.out.println(key + ":" + currentGraphPath.peek().get(key));
        }
    }
}
