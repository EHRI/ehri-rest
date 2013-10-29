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
 *
 * makes use of icaatom.properties with format: part/of/path/=attribute
 *
 * @author linda
 */
public class BundesarchiveEadHandler extends EadHandler {

    private static int bundesarchivecount = 0;
    private static final Logger logger = LoggerFactory
            .getLogger(BundesarchiveEadHandler.class);
    private final List<DocumentaryUnit>[] children = new ArrayList[12];
    // Pattern for EAD nodes that represent a child item
//    private Pattern childItemPattern = Pattern.compile("^/*c(?:\\d*)$");
    

//    /**
//     * Set a custom resolver so EAD DTDs are never looked up online.
//     * @param publicId
//     * @param systemId
//     * @return returns essentially an empty dtd file
//     * @throws org.xml.sax.SAXException
//     * @throws java.io.IOException
//     */
//    public org.xml.sax.InputSource resolveEntity(String publicId, String systemId)
//            throws org.xml.sax.SAXException, java.io.IOException {
//        // This is the equivalent of returning a null dtd.
//        return new org.xml.sax.InputSource(new java.io.StringReader(""));
//    }

    @SuppressWarnings("unchecked")
    public BundesarchiveEadHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("bundesarchive.properties"));
        children[depth] = new ArrayList<DocumentaryUnit>();
    }

//    @Override
//    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
//        super.startElement(uri, localName, qName, attributes);
//
//        if (childItemPattern.matcher(qName).matches() || qName.equals("archdesc")) { //a new DocumentaryUnit should be created
//            children[depth] = new ArrayList<DocumentaryUnit>();
//        }
////        for (int attr = 0; attr < attributes.getLength(); attr++) { // only certain attributes get stored
////            if (p.hasAttributeProperty(attributes.getLocalName(attr))) {
////                putPropertyInCurrentGraph(p.getAttributeProperty(attributes.getLocalName(attr)), attributes.getValue(attr));
////            }
////        }
//    }

//    @Override
//    public void endElement(String uri, String localName, String qName) throws SAXException {
//        //the child closes, add the new DocUnit to the list, establish some relations
//        super.endElement(uri, localName, qName);
//
//        if (needToCreateSubNode(qName)) {
//            Map<String, Object> currentGraph = currentGraphPath.pop();
//            if (childItemPattern.matcher(qName).matches() || qName.equals("archdesc")) {
//                try {
//                    //add any mandatory fields not yet there:
//                	
//                    extractIdentifier(currentGraph);
//                    extractTitle(currentGraph);
//                    
//                    setDefaultLanguage(currentGraph);
//                    
//                    extractDate(currentGraph);
//
//                    DocumentaryUnit current = (DocumentaryUnit)importer.importItem(currentGraph, depth);
//                    logger.debug("importer used: " + importer.getClass());
//                    if (depth > 0) { // if not on root level
//                    	children[depth - 1].add(current); // add child to parent offspring
//                    	//set the parent child relationships by hand, as we don't have the parent Documentary unit yet.
//                    	//only when closing a DocUnit, one can set the relationship to its children, but not its parent, as that has not yet been closed.
//                    	for (DocumentaryUnit child : children[depth]) {
//                    		if (child != null) {
//                    			current.addChild(child);
//                    			// FIXME: Is this correct??? It should be done automatically
//                    			// using the scope of the BundleDAO, but because the actual
//                    			// parent doesn't exist, we have to override it and set
//                    			// this here...
//                    			child.setPermissionScope(current);
//                    		}
//                    	}
//                    }
//                } catch (ValidationError ex) {
//                    logger.error(ex.getMessage());
//                } finally {
//                    depth--;
//                }
//            } else {
//                putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
//                depth--;
//            }
//        }
//
//        currentPath.pop();
//    }

    @Override
	protected void useDefaultLanguage(Map<String, Object> currentGraph) {
		// Use "de" as default language
		useDefaultLanguage(currentGraph, "de");
	}

    @Override
	protected void extractTitle(Map<String, Object> currentGraph) {
		//the BA have multiple unittitles, but with different types, find the Bestandsbezeichnung
		if (!currentGraph.containsKey(Ontology.NAME_KEY)) {
		    //finding some name for this unit:
		    logger.error("Bundesarchiv node without name field: ");
		    currentGraph.put(Ontology.NAME_KEY, "UNKNOWN title" );
		} else if(currentGraph.get(Ontology.NAME_KEY) instanceof List){
		        logger.debug("class of identifier: " + currentGraph.get(Ontology.NAME_KEY).getClass());
		        ArrayList<String> names = (ArrayList<String>) currentGraph.get(Ontology.NAME_KEY);
		        ArrayList<String> nametypes = (ArrayList<String>) currentGraph.get(Ontology.NAME_KEY+"Type");
		        for (int i = 0; i < names.size(); i++) {
		            if (nametypes.get(i).equals("Bestandsbezeichnung")) {
		                logger.debug("found official name: " + names.get(i));
		                currentGraph.put(Ontology.NAME_KEY, names.get(i));
		            } else {
		                logger.debug("found other form of name: " + names.get(i));
		                currentGraph.put("otherFormsOfName", names.get(i));
		            }
		        }
		        currentGraph.remove(Ontology.NAME_KEY+"Type");
		}
	}

    @Override
	protected void extractDate(Map<String, Object> currentGraph) {
		//the BA have multiple unitdates, but with different types, find the Bestandslaufzeit
		    // there is a difference between Ontology.ENTITY_HAS_DATE and the .properties-defined term for unit dates
		    final String UNITDATE = "unitDates";  
		    
		    if (!currentGraph.containsKey(UNITDATE)) {
		        //finding some date for this unit:
		        logger.error("Bundesarchiv node without unitdate field: ");
		        currentGraph.put(Ontology.ENTITY_HAS_DATE, "UNKNOWN date" );
		    } else if(currentGraph.get(UNITDATE) instanceof List){
		            logger.debug("class of dates: " + currentGraph.get(UNITDATE).getClass());
		            ArrayList<String> dates = (ArrayList<String>) currentGraph.get(UNITDATE);
		            ArrayList<String> datetypes = (ArrayList<String>) currentGraph.get(UNITDATE+"Types");
		            for (int i = 0; i < dates.size(); i++) {
		                if (datetypes.get(i).equals("Bestandslaufzeit")) {
		                    logger.debug("found archival date: " + dates.get(i));
		                    currentGraph.put(UNITDATE, dates.get(i));
		                } else {
		                    logger.debug("found other type of date: " + dates.get(i));
		                    currentGraph.put("otherFormsOfDate", dates.get(i));
		                }
		            }
		            currentGraph.remove(UNITDATE+"Types"); // Why only when ..Type(s) is a list?
		    }
	}

    @Override
	protected void extractIdentifier(Map<String, Object> currentGraph) {
		//not all units have ids, and some have multiple, find the Bestandssignatur
		if (currentGraph.containsKey("objectIdentifier")) {
		    if (currentGraph.get("objectIdentifier") instanceof List) {
		        logger.debug("class of identifier: " + currentGraph.get("objectIdentifier").getClass());
		        ArrayList<String> identifiers = (ArrayList<String>) currentGraph.get("objectIdentifier");
		        ArrayList<String> identifiertypes = (ArrayList<String>) currentGraph.get("objectIdentifierType");
		        for (int i = 0; i < identifiers.size(); i++) {
		            if (identifiertypes.get(i).equals("Bestandssignatur")) {
		                logger.debug("found official id: " + identifiers.get(i));
		                currentGraph.put("objectIdentifier", identifiers.get(i));
		            }else {
		                logger.debug("found other form of identifier: " + identifiers.get(i));
		                currentGraph.put("arta", identifiers.get(i));
		            }
		        }
		        currentGraph.remove("objectIdentifierType");
		    }
		} else {
		    logger.error("no unitid found, setting " + ++bundesarchivecount);
		    currentGraph.put("objectIdentifier", "bundesarchiveID"+bundesarchivecount);
		    
		}
	}

//    @Override
//    protected boolean needToCreateSubNode(String qName) {
//        //child or parent unit:
//        return childItemPattern.matcher(qName).matches() || qName.equals("archdesc");
//    }
//
//    @Override
//    protected List<String> getSchemas() {
//        List<String> schemas = new ArrayList<String>();
//        schemas.add("xlink.xsd");
//        schemas.add("ead.xsd");
//        return schemas;
//    }
}
