package eu.ehri.project.importers;

import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Handler of EAD files. Use to create a representation of the structure of Documentary Units.
 * This generic handler does not do tricks to get data from any CHI-custom use of EAD - you
 * should extend this class for that.
 * If there is no language, it does set the language of the description to English.
 * makes use of icaatom.properties with format: part/of/path/=attribute
 *
 * @author linda
 * @author ben
 */
public class EadHandler extends SaxXmlHandler {

    private static final Logger logger = LoggerFactory
            .getLogger(EadHandler.class);
    protected final List<DocumentaryUnit>[] children = new ArrayList[12];
    protected final Stack<String> scopeIds = new Stack<String>();
    // Pattern for EAD nodes that represent a child item
    private final static Pattern childItemPattern = Pattern.compile("^/*c(?:\\d*)$");

    // Constants for elements we need to watch for.
    private final static String ARCHDESC = "archdesc";
    private final static String DID = "did";

    /**
     * Default language to use in units without language
     */
    protected String defaultLanguage = "eng";
    
    /**
     * EAD identifier as found in `<eadid>` in the currently handled EAD file
     */
    private String eadId;

    /**
     * Set a custom resolver so EAD DTDs are never looked up online.
     */
    public org.xml.sax.InputSource resolveEntity(String publicId, String systemId)
            throws org.xml.sax.SAXException, java.io.IOException {
        // This is the equivalent of returning a null dtd.
        return new org.xml.sax.InputSource(new java.io.StringReader(""));
    }

    /**
     * Create an EadHandler using some importer. The default mapping of paths to node properties is used.
     * 
     * @param importer
     */
    @SuppressWarnings("unchecked")
    public EadHandler(AbstractImporter<Map<String, Object>> importer) {
        this(importer, new XmlImportProperties("icaatom.properties"));
    }

    /**
     * Create an EadHandler using some importer, and a mapping of paths to node properties.
     * 
     * @param importer
     * @param xmlImportProperties
     */
    public EadHandler(AbstractImporter<Map<String, Object>> importer,
            XmlImportProperties xmlImportProperties) {
        super(importer, xmlImportProperties);
        children[depth] = Lists.newArrayList();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (isUnitDelimiter(qName)) { //a new DocumentaryUnit should be created
            children[depth] = Lists.newArrayList();
        }
    }

    protected List<String> pathIds() {
        if (scopeIds.isEmpty()) {
            return scopeIds;
        } else {
            List<String> path = Lists.newArrayList();
            for (int i = 0; i < scopeIds.size() - 1; i++) {
                path.add(scopeIds.get(i));
            }
            return path;
        }

    }

    private String getCurrentTopIdentifier() {
        Object current = currentGraphPath.peek().get(OBJECT_IDENTIFIER);
        if (current instanceof List<?>) {
            return (String) ((List) current).get(0);
        } else {
            return (String) current;
        }
    }

    /**
	 * Called when the XML parser encounters an end tag. This is tuned for EAD files, which come in many flavours.
	 * 
	 * Certain elements represent subcollections, for which we create new nodes (here, we create representative Maps for nodes).
	 * Many EAD producers use the standard in their own special way, so this method calls generalised methods to filter, get data 
	 * in the right place and reformat. 
	 * If a collection of EAD files need special treatment to get specific data in the right place, you only need to override the 
	 * other methods (in order: extractIdentifier, extractTitle, extractDate). 
	 */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //the child closes, add the new DocUnit to the list, establish some relations
        super.endElement(uri, localName, qName);
        
    	// If this is the <eadid> element, store its content
//    	logger.debug("localName: " + localName + ", qName: " + qName);
    	if (localName.equals("eadid") || qName.equals("eadid")) {
    		eadId = (String) currentGraphPath.peek().get("eadId");
    		logger.debug("Found <eadid>: "+ eadId);
    	}

        // FIXME: We need to add the 'parent' identifier to the ID stack
        // so that graph path IDs are created correctly. This currently
        // assumes there's a 'did' element from which we extract this
        // identifier.
        if (qName.equals(DID)) {
            extractIdentifier(currentGraphPath.peek());
            String topId = getCurrentTopIdentifier();
            scopeIds.push(topId);
            logger.debug("Current id path: " + scopeIds);
        }

        if (needToCreateSubNode(qName)) {
            Map<String, Object> currentGraph = currentGraphPath.pop();

            if (isUnitDelimiter(qName)) {
                try {
                    //add any mandatory fields not yet there:
                    // First: identifier(s),
                    extractIdentifier(currentGraph);

                    // Second: title
                    extractTitle(currentGraph);
                    
//                    extractEadLanguage(currentGraph);
                    

                    useDefaultLanguage(currentGraph);
                    extractDate(currentGraph);

                    DocumentaryUnit current = (DocumentaryUnit) importer.importItem(currentGraph, pathIds());
                    logger.debug("importer used: " + importer.getClass());
                    if (depth > 0) { // if not on root level
                        children[depth - 1].add(current); // add child to parent offspring
                        // set the parent child relationships by hand
                        // as we don't have the parent Documentary unit yet.
                        // only when closing a DocUnit, one can set the relationship to its children,
                        // but not its parent, as that has not yet been closed.
                        for (DocumentaryUnit child : children[depth]) {
                            if (child != null) {
                                current.addChild(child);
                                child.setPermissionScope(current);
                            }
                        }
                    }
                } catch (ValidationError ex) {
                    logger.error("caught validation error: " + ex.getMessage());
                } finally {
                    depth--;
                    scopeIds.pop();
                }
            } else {
                putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
                depth--;
            }
        }

        currentPath.pop();
    }
    
    /**
     * Get the EAD identifier of the EAD being imported
     * @return the <code><eadid></code> or null if it was not parsed yet or empty
     */
    protected String getEadId() {
    	return eadId;
    }

    /**
     * Handler-specific code for extraction or generation of description languages.
	 * Default method is empty; override when necessary.
     * @param currentGraph
     */
    protected void extractEadLanguage(Map<String, Object> currentGraph) {
		// TODO Auto-generated method stub
		
	}

	/**
     * Checks given currentGraph for a language and sets a default language code
     * for the description if no language is found.
     *
     * @param currentGraph Data at the current node level
     */
    protected void useDefaultLanguage(Map<String, Object> currentGraph) {
        useDefaultLanguage(currentGraph, getDefaultLanguage());
    }

    /**
     * Checks given currentGraph for a language and sets a default language code
     * for the description if no language is found.
     *
     * @param currentGraph    Data at the current node level
     * @param defaultLanguage Language code to use as default
     */
    protected void useDefaultLanguage(Map<String, Object> currentGraph, String defaultLanguage) {

        if (!currentGraph.containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
            logger.debug("Using default language code: " + defaultLanguage);
            currentGraph.put(Ontology.LANGUAGE_OF_DESCRIPTION, defaultLanguage);
        }
    }

    protected String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Handler-specific code for extraction or generation of unit titles.
     * Default method is empty; override when necessary.
     *
     * @param currentGraph Data at the current node level
     */
    protected void extractTitle(Map<String, Object> currentGraph) {

    }

    /**
     * Handler-specific code for extraction or generation of unit dates.
     * Default method is empty; override when necessary.
     *
     * @param currentGraph Data at the current node level
     */
    protected void extractDate(Map<String, Object> currentGraph) {

    }

    /**
     * Handler-specific code for extraction or generation of unit IDs.
     * Default method is empty; override when necessary.
     *
     * @param currentGraph Data at the current node level
     */
    protected void extractIdentifier(Map<String, Object> currentGraph) {

    }

    /**
     * Helper method to add identifiers to the list of other identifiers.
     * The property named Ontology.OTHER_IDENTIFIERS (i.e. "otherIdentifiers")
     * is always an ArrayList of Strings.
     *
     * @param currentGraph    the node representation to add the otherIdentifier to
     * @param otherIdentifier the alternative identifier to add
     */
    protected void addOtherIdentifier(Map<String, Object> currentGraph, String otherIdentifier) {
        if (currentGraph.containsKey(Ontology.OTHER_IDENTIFIERS)) {
            logger.debug("adding alternative id: " + otherIdentifier);
            Object oids = currentGraph.get(Ontology.OTHER_IDENTIFIERS);
            if (oids != null && oids instanceof ArrayList<?>) {
                ((ArrayList<String>) oids).add(otherIdentifier);
            	logger.debug("alternative ID added");
            }
        } else {
            logger.debug("adding first alt id: " + otherIdentifier);
            ArrayList<String> oids = new ArrayList<String>();
            oids.add(otherIdentifier);
            currentGraph.put(Ontology.OTHER_IDENTIFIERS, oids);
        }
    }

    @Override
    protected boolean needToCreateSubNode(String qName) {
        //child or parent unit:
        boolean need = isUnitDelimiter(qName);
        //controlAccess 
        String path = getImportantPath(currentPath);
        if (path != null) {
            need = need || path.endsWith("Access");
        }
        return need;
    }

    @Override
    protected List<String> getSchemas() {
        List<String> schemas = new ArrayList<String>();
        schemas.add("xlink.xsd");
        schemas.add("ead.xsd");
        return schemas;
    }

    /**
     * Determine if the element represents a unit delimiter
     *
     * @param elementName The XML element name
     * @return Whether or not we're moved to a new item
     */
    protected static boolean isUnitDelimiter(String elementName) {
        return childItemPattern.matcher(elementName).matches() || elementName.equals(ARCHDESC);
    }
}
