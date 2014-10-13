package eu.ehri.project.importers;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import eu.ehri.project.importers.properties.XmlImportProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import eu.ehri.project.importers.util.Helpers;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import static eu.ehri.project.definitions.Ontology.LANGUAGE_OF_DESCRIPTION;

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
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public abstract class SaxXmlHandler extends DefaultHandler implements LexicalHandler {

    private static final Logger logger = LoggerFactory.getLogger(SaxXmlHandler.class);
    
    /**
     * Keys in the graph that encode a language code must start with the LANGUAGE_KEY_PREFIX.
     */
    public static final String LANGUAGE_KEY_PREFIX = "language";
    
    /**
     * Keys in the node that denote unknown properties must start with the value of UNKNOWN.
     */
    public static final String UNKNOWN = "UNKNOWN_";
    
    /**
     * Key in the node that denotes the object's identifier. 
     */
    public static final String OBJECT_IDENTIFIER = "objectIdentifier";
    protected final Stack<Map<String, Object>> currentGraphPath = new Stack<Map<String, Object>>();
    protected final Map<String, Map<String, Object>> languageMap = Maps.newHashMap();
    protected final Stack<String> currentPath = new Stack<String>();
    protected final Stack<StringBuilder> currentText = new Stack<StringBuilder>();

    protected String currentEntity = null;

    protected final AbstractImporter<Map<String, Object>> importer;
    protected final XmlImportProperties properties;

    protected int depth = 0;
    String attribute=null;

    /**
     * 
     */
    private String languagePrefix;

    /**
     * Get a List of Strings containing files names of XML Schemas.
     * @return List containing files names as Strings
     */
    protected abstract List<String> getSchemas();

    public SaxXmlHandler(AbstractImporter<Map<String, Object>> importer, XmlImportProperties properties) {
        super();
        this.importer = importer;
        this.properties = properties;
        currentGraphPath.push(new HashMap<String, Object>());
    }

    /**
     * Determines whether a new node that is a 'child' of the current node (i.e. a 'sub-node')
     * needs to be created, based on the qualified element name.
     * @param qName the QName
     * @return true if the QName warrants a sub-node, false otherwise
     */
    protected abstract boolean needToCreateSubNode(String qName);

    @Override
    public void startEntity(String name) {
        currentEntity = name;
    }

    @Override
    public void endEntity(String name) {
        currentEntity = null;
    }

    @Override public void startDTD(String name,String publicId,String systemId) {}
    @Override public void endDTD() {}
    @Override public void comment(char[] ch, int start, int end) {}
    @Override public void startCDATA() {}
    @Override public void endCDATA() {}


    /**
     * Receive an opening tag. Initialise the current text to store the characters,
     * create a language map to hold descriptions in different languages,
     * push the level if this element warrants a new sub-node and store the attributes 
     * that need to be stored.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // initialise the text holding space
    	currentText.push(new StringBuilder());

    	// retrieve the language from the attributes and
    	// create a language map
        Optional<String> lang = languageAttribute(attributes);
        if (lang.isPresent()) {
            languagePrefix = lang.get();
            if (!languageMap.containsKey(languagePrefix)) {
                if (languageMap.isEmpty()) {
                    currentGraphPath.peek().put(LANGUAGE_OF_DESCRIPTION, languageMap);
                }
                Map<String, Object> m = new HashMap<String, Object>();
                m.put(LANGUAGE_OF_DESCRIPTION, languagePrefix);
                languageMap.put(languagePrefix, m);
            }
        }

        // Update the path with the new element name
        currentPath.push(withoutNamespace(qName));
        if (needToCreateSubNode(qName)) { //a new subgraph should be created
            depth++;
            logger.debug("Pushing depth... " + depth + " -> " + qName);
            currentGraphPath.push(new HashMap<String, Object>());
        }

        // Store attributes that are listed in the .properties file
        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
            String attributeName = withoutNamespace(attributes.getQName(attr));
            if (properties.hasAttributeProperty(attributeName)
                    && !properties.getAttributeProperty(attributeName).equals(LANGUAGE_OF_DESCRIPTION)) {
              
                if (isKeyInPropertyFile(currentPath, "@" + properties.getAttributeProperty(attributeName), "")) {
                    String path = getImportantPath(currentPath, "@" + properties.getAttributeProperty(attributeName), "");
                    putPropertyInCurrentGraph(path, attributes.getValue(attr));
                } else if (isKeyInPropertyFile(currentPath, "@" + properties.getAttributeProperty(attributeName), "$" + attributes.getValue(attr))) {
                    this.attribute = getImportantPath(currentPath, "@" + properties.getAttributeProperty(attributeName), "$" + attributes.getValue(attr));
                } else{
                    logger.debug("attribute " + attributeName + " not found in properties");
                }
            }
        }
    }

    /**
     * Receive an end element. Put the contents of the text holding space in the current graph
     * as a property using the .properties file as a mapping.
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (languagePrefix == null) {
            if(attribute == null){
                putPropertyInCurrentGraph(getImportantPath(currentPath), currentText.pop().toString());
            }else{
                putPropertyInCurrentGraph(attribute, currentText.pop().toString());
                attribute= null;
            }
        } else {
            putPropertyInGraph(languageMap.get(languagePrefix), getImportantPath(currentPath), currentText.pop().toString());
        }



    }


    /**
     * Insert a graph representation in the current graph (the one on top of 
     * the currentGraphPath stack) as a list item at the given key.
     * The value stored at the key is always a list - if the key did not exist
     * yet, a new list is created to which the sub-graph is added.
     * 
     * @param key name of the edge to connect the sub-graph to the current graph
     * @param subgraph Map graph representation to insert into the current graph
     */
    @SuppressWarnings("unchecked")
    protected void putSubGraphInCurrentGraph(String key, Map<String, Object> subgraph) {
        Map<String, Object> c = currentGraphPath.peek();
        if (c.containsKey(key)) {
            ((List<Map<String, Object>>) c.get(key)).add(subgraph);
        } else {
            List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
            l.add(subgraph);
            c.put(key, l);
        }
    }

    /**
     * Get the language from the XML attributes, if it is there.
     * Else return 'absent'.
     * @param attributes SAX-parsed XML attributes
     * @return an Optional containing the language, or 'absent'
     */
    private Optional<String> languageAttribute(Attributes attributes) {
        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
            String isLangAttribute = withoutNamespace(attributes.getQName(attr));
            String prop = properties.getAttributeProperty(isLangAttribute);
            if (prop != null && prop.equals(LANGUAGE_OF_DESCRIPTION)) {
                logger.debug("Language detected!");
                return Optional.of(attributes.getValue(attr));
            }
        }
        return Optional.absent();
    }

    /**
     * Get the element name without namespace prefix.
     * 
     * @param qName an element QName that may have a namespace prefix
     * @return the element name without namespace prefix
     */
    protected String withoutNamespace(String qName) {
        String name = qName;
        int colon = qName.indexOf(":");
        if (colon > -1) {
//            assert(qName.substring(0, colon).equals(prefixStack.peek()));
            name = qName.substring(colon + 1);
        }
        return name;
    }

    /**
     * Receives character data in SAX events, converts multiple space to a single space
     * and puts the characters in the current node, unless the input contains only 
     * whitespace.
     */
    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        // NB: 'Blank' (whitespace) only strings are significant here, because
        // otherwise a sequence of character, line-break, and an entity will
        // end up being concatenated with the line-break removed. We therefore
        // preserve all line breaks and other whitespace here and normalize
        // it when the text gets added to the graph.
        currentText.peek().append(ch, start, length);
    }

    /**
     * Stores this property value pair in the current DocumentNode.
     * If the property already exists, it is added to the value list.
     *
     * @param property
     * @param value
     */
    protected void putPropertyInCurrentGraph(String property, String value) {
        putPropertyInGraph(currentGraphPath.peek(), property, value);
    }

    /**
     * Stores this property value pair in the given graph node representation.
     * If the value is effectively empty, nothing happens.
     * If the property already exists, it is added to the value list.
     *
     * @param c a Map representation of a graph node
     * @param property the key to store the value for
     * @param value the value to store
     */
    protected static void putPropertyInGraph(Map<String, Object> c, String property, String value) {
        if (value == null)
            return;
        String valuetrimmed = value.trim();
        if (valuetrimmed.isEmpty()) {
            return;
        }

        // Language properties 

        if (property.startsWith(LANGUAGE_KEY_PREFIX)) {
            valuetrimmed = Helpers.iso639DashTwoCode(valuetrimmed);
        }

        valuetrimmed = StringUtils.normalizeSpace(valuetrimmed);

        logger.debug("putProp: " + property + " " + valuetrimmed);

        Object propertyList;
        if (c.containsKey(property)) {
            propertyList = c.get(property);
            if (propertyList instanceof List) {
                ((List<Object>) propertyList).add(valuetrimmed);
            } else {
                List<Object> o = new ArrayList<Object>();
                o.add(c.get(property));
                o.add(valuetrimmed);
                c.put(property, o);
            }
        } else {
            c.put(property, valuetrimmed);
        }
    }
    
    /**
     * Overwrite a value in the current graph.
     * 
     * @param property name of the property to overwrite
     * @param value new value for the property.
     */
    protected void overwritePropertyInCurrentGraph(String property, Object value){
        overwritePropertyInCurrentGraph(currentGraphPath.peek(), property, value);
    }
    
    private void overwritePropertyInCurrentGraph(Map<String, Object> c,String property, Object value){
        logger.debug("overwriteProp: " + property + " " + value);
        c.put(property, value);
    }

    /**
     * Get the property name corresponding to the given path from the .properties file.
     * 
     * @param path the stacked element names forming a path from the root to the current element
     * @return the corresponding value to this path from the properties file. the search is inside out, so if
     * both eadheader/ and ead/eadheader/ are specified, it will return the value for the first
     *
     * if this path has no corresponding value in the properties file, it will return the entire path name, with _
     * replacing the /
     */
    protected String getImportantPath(Stack<String> path) {
        return getImportantPath(path, "", "");
    }
    
    /**
     * did/unitid/@ehrilabel$ehri_main_identifier=objectIdentifier
     * 
     * @param path did/unitid/
     * @param attribute @ehrilabel
     * @param attributevalue $ehri_main_identifier
     * 
     * @return the corresponding value to this path from the properties file. The search is inside out, so if
     * both eadheader/ and ead/eadheader/ are specified, it will return the value for the first.
     *
     * If this path has no corresponding value in the properties file, it will return the entire path name, with _
     * replacing the /
     */
    private String getImportantPath(Stack<String> path, String attribute, String attributevalue) {
        
        String all = "";
        for (int i = path.size(); i > 0; i--) {
            all = path.get(i - 1) + "/" + all;
            if (properties.getProperty(all+attribute+attributevalue) != null) {
                return properties.getProperty(all+attribute+attributevalue);
            }
        }
        return UNKNOWN + all.replace("/", "_");
    }
    /**
     * did/unitid/@ehrilabel$ehri_main_identifier=objectIdentifier
     * 
     * @param path did/unitid/
     * @param attribute @ehrilabel
     * @param attributevalue $ehri_main_identifier
     * 
     * @return returns true if this path is a key in the properties file. 
     *
     * If this path has no corresponding value in the properties file, it will return false
     */
    private boolean isKeyInPropertyFile(Stack<String> path, String attribute, String attributevalue) {
        
        String all = "";
        for (int i = path.size(); i > 0; i--) {
            all = path.get(i - 1) + "/" + all;
            if (properties.getProperty(all+attribute+attributevalue) != null) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Print a text representation of the graph on `System.out`.
     */
    protected void printGraph() {
        for (String key : currentGraphPath.peek().keySet()) {
            System.out.println(key + ":" + currentGraphPath.peek().get(key));
        }
    }
}
