package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    // Pattern for EAD nodes that represent a child item
    protected Pattern childItemPattern = Pattern.compile("^/*c(?:\\d*)$");
    
    /**
     * Default language to use in units without language
     */
    protected String defaultLanguage = "eng";
    

    /**
     * Set a custom resolver so EAD DTDs are never looked up online.
     * @param publicId
     * @param systemId
     * @return returns essentially an empty dtd file
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     */
    public org.xml.sax.InputSource resolveEntity(String publicId, String systemId)
            throws org.xml.sax.SAXException, java.io.IOException {
        // This is the equivalent of returning a null dtd.
        return new org.xml.sax.InputSource(new java.io.StringReader(""));
    }

    @SuppressWarnings("unchecked")
    public EadHandler(AbstractImporter<Map<String, Object>> importer) {
        this(importer, new XmlImportProperties("icaatom.properties"));
    }

    public EadHandler(AbstractImporter<Map<String, Object>> importer,
			XmlImportProperties xmlImportProperties) {
		super(importer,xmlImportProperties);
        children[depth] = new ArrayList<DocumentaryUnit>();
	}

	@Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (childItemPattern.matcher(qName).matches() || qName.equals("archdesc")) { //a new DocumentaryUnit should be created
            children[depth] = new ArrayList<DocumentaryUnit>();
        }
//        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
//            if (p.hasAttributeProperty(attributes.getLocalName(attr))) {
//                putPropertyInCurrentGraph(p.getAttributeProperty(attributes.getLocalName(attr)), attributes.getValue(attr));
//            }
//        }
    }

	/**
	 * Called when the XML parser encounters an end tag. This is tuned for EAD files, which come in many flavours.
	 * 
	 * Certain elements represent subcollections, for which we create new nodes (here, we create representative Maps for nodes).
	 * Many EAD producers use the standard in their own special way, so this method calls generalised methods to filter, get data 
	 * in the right place and reformat. 
	 * If a collection of EAD files need special treatment to get specific data in the right place, you only need to override the 
	 * other methods (extractTitle, extractIdentifier, extractDate). 
	 */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //the child closes, add the new DocUnit to the list, establish some relations
        super.endElement(uri, localName, qName);

        if (needToCreateSubNode(qName)) {
            Map<String, Object> currentGraph = currentGraphPath.pop();
            if (childItemPattern.matcher(qName).matches() || qName.equals("archdesc")) {
                try {
                    //add any mandatory fields not yet there:
                	// First: identifier(s), 
                    extractIdentifier(currentGraph);
                    // 
                    extractTitle(currentGraph);
                    
                    useDefaultLanguage(currentGraph);
                    
                    extractDate(currentGraph);

                    DocumentaryUnit current = (DocumentaryUnit)importer.importItem(currentGraph, depth);
                    logger.debug("importer used: " + importer.getClass());
                    if (depth > 0) { // if not on root level
                    	children[depth - 1].add(current); // add child to parent offspring
                    	//set the parent child relationships by hand, as we don't have the parent Documentary unit yet.
                    	//only when closing a DocUnit, one can set the relationship to its children, but not its parent, as that has not yet been closed.
                    	for (DocumentaryUnit child : children[depth]) {
                    		if (child != null) {
                    			current.addChild(child);
                    			// FIXME: Is this correct??? It should be done automatically
                    			// using the scope of the BundleDAO, but because the actual
                    			// parent doesn't exist, we have to override it and set
                    			// this here...
                    			child.setPermissionScope(current);
                    		}
                    	}
                    }
                } catch (ValidationError ex) {
                    logger.error("caught validation error: " + ex.getMessage());
                } finally {
                    depth--;
                }
            } else {
                putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
                depth--;
            }
        }

        currentPath.pop();
    }

    /**
     * Checks given currentGraph for a language and sets a default language code
     * for the description if no language is found.
     * @param currentGraph
     */
	protected void useDefaultLanguage(Map<String, Object> currentGraph) {
		useDefaultLanguage(currentGraph, getDefaultLanguage());
	}
	
	/**
     * Checks given currentGraph for a language and sets a default language code
     * for the description if no language is found.
     * @param currentGraph
     * @param defaultLanguage Language code to use as default
     */
	protected void useDefaultLanguage(Map<String, Object> currentGraph, String defaultLanguage) {
		
		if (!currentGraph.containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
			logger.debug("Using default language code: " +defaultLanguage);
		    currentGraph.put(Ontology.LANGUAGE_OF_DESCRIPTION, defaultLanguage);
		}
	}
	
	protected String getDefaultLanguage() {
		return defaultLanguage;
	}

	/**
	 * Handler-specific code for extraction or generation of unit titles.
	 * Default method is empty; override when necessary.
	 * @param currentGraph
	 */
	protected void extractTitle(Map<String, Object> currentGraph) {
		
	}

	/**
	 * Handler-specific code for extraction or generation of unit dates.
	 * Default method is empty; override when necessary.
	 * @param currentGraph
	 */
	protected void extractDate(Map<String, Object> currentGraph) {
		
	}

	/**
	 * Handler-specific code for extraction or generation of unit IDs.
	 * Default method is empty; override when necessary.
	 * @param currentGraph
	 */
	protected void extractIdentifier(Map<String, Object> currentGraph) {
		
	}

	/**
	 * Helper method to add identifiers to the list of other identifiers.
	 * The property named Ontology.OTHER_IDENTIFIERS (i.e. "otherIdentifiers")
	 * is always an ArrayList of Strings.
	 * @param currentGraph the node representation to add the otherIdentifier to
	 * @param otherIdentifier the alternative identifier to add
	 */
	protected void addOtherIdentifier(Map<String, Object> currentGraph, String otherIdentifier) {
		if (currentGraph.containsKey(Ontology.OTHER_IDENTIFIERS)) {
			logger.debug("adding alternative id: " + otherIdentifier);
			ArrayList<String> oids = (ArrayList<String>) currentGraph.get(Ontology.OTHER_IDENTIFIERS);
			oids.add(otherIdentifier);
		}
		else {
			logger.debug("adding first alt id: " + otherIdentifier);
			ArrayList<String> oids = new ArrayList<String>();
			oids.add(otherIdentifier);
			currentGraph.put(Ontology.OTHER_IDENTIFIERS, oids);
		}
	}
	
    @Override
    protected boolean needToCreateSubNode(String qName) {
        //child or parent unit:
    	boolean need = childItemPattern.matcher(qName).matches() || qName.equals("archdesc");
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
}
