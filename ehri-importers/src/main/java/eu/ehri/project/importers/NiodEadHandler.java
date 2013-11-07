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
 * makes use of niodead.properties with format: part/of/path/=attribute
 *
 * @author linda
 * @author ben
 */
public class NiodEadHandler extends EadHandler {

    private static int niodcount = 0;
    private static final Logger logger = LoggerFactory
            .getLogger(NiodEadHandler.class);
    
    private String[] ids = new String[13];
    private String[] fullids = new String[13];
    private String eadid;

    @SuppressWarnings("unchecked")
    public NiodEadHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("niodead.properties"));
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    	super.startElement(uri, localName, qName, attributes);
    	
//    	if (needToCreateSubNode(qName)){
//    		String unitid = (String) currentGraphPath.peek().get("objectIdentifier");
//    		logger.debug("Unitid at depth "+ depth + ": " + unitid);
//    		ids[depth] = unitid;
//    		fullids[depth] = fullids[depth-1] + unitid;
//    	}
    }
//    
//    @Override
//    public void characters(char ch[], int start, int length) throws SAXException {
//    	super.characters(ch, start, length);
//    	
//    }
//    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //the child closes, add the new DocUnit to the list, establish some relations
        super.endElement(uri, localName, qName);
        if (qName.equals("eadid")) {
        	eadid = (String) currentGraphPath.peek().get("eadIdentifier");
        	ids[0] = eadid;
        	fullids[0] = eadid;
        	logger.debug("eadid set: " + eadid);
        }
        else if (qName.equals("unitid")) {
        	String uid = (String) currentGraphPath.peek().get("objectIdentifier");
        	ids[depth] = uid;
        	fullids[depth] = fullids[depth-1] + uid;
        	logger.debug("unitid at depth " + depth +": " + uid);
        }
    	

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
    }

    @Override
	protected void useDefaultLanguage(Map<String, Object> currentGraph) {
		// Use "nl" as default language
		useDefaultLanguage(currentGraph, "nl");
	}

    @Override
	protected void extractTitle(Map<String, Object> currentGraph) {
		// NIOD usually has a title, but not for every sub-unit
    	
		if (!currentGraph.containsKey(Ontology.NAME_KEY)) {
		    //finding some name for this unit:
		    logger.error("NIOD node without unittitle field: ");
		    currentGraph.put(Ontology.NAME_KEY, "UNKNOWN title" );
		} 
//    	else if(currentGraph.get(Ontology.NAME_KEY) instanceof List){
//		        logger.debug("class of identifier: " + currentGraph.get(Ontology.NAME_KEY).getClass());
//		        ArrayList<String> names = (ArrayList<String>) currentGraph.get(Ontology.NAME_KEY);
//		        ArrayList<String> nametypes = (ArrayList<String>) currentGraph.get(Ontology.NAME_KEY+"Type");
//		        for (int i = 0; i < names.size(); i++) {
//		            if (nametypes.get(i).equals("Bestandsbezeichnung")) {
//		                logger.debug("found official name: " + names.get(i));
//		                currentGraph.put(Ontology.NAME_KEY, names.get(i));
//		            } else {
//		                logger.debug("found other form of name: " + names.get(i));
//		                currentGraph.put("otherFormsOfName", names.get(i));
//		            }
//		        }
//		        currentGraph.remove(Ontology.NAME_KEY+"Type");
//		}
	}

    @Override
	protected void extractDate(Map<String, Object> currentGraph) {
		//the BA have multiple unitdates, but with different types, find the Bestandslaufzeit
    	// there is a difference between Ontology.ENTITY_HAS_DATE and the .properties-defined term for unit dates
    	final String UNITDATE = "unitDates";  

    	if (!currentGraph.containsKey(UNITDATE)) {
    		//finding some date for this unit:
    		logger.error("NIOD node without unitdate field: ");
    		currentGraph.put(Ontology.ENTITY_HAS_DATE, "UNKNOWN date" );
    	} 
    	else if (currentGraph.get(UNITDATE).equals("0/9999")) {
    		logger.info("NIOD node with bad date '0/9999', unsetting date");
    		currentGraph.put(Ontology.ENTITY_HAS_DATE, "UNKNOWN date" );
    	}
//    	else if(currentGraph.get(UNITDATE) instanceof List){
//    		logger.debug("class of dates: " + currentGraph.get(UNITDATE).getClass());
//    		ArrayList<String> dates = (ArrayList<String>) currentGraph.get(UNITDATE);
//    		ArrayList<String> datetypes = (ArrayList<String>) currentGraph.get(UNITDATE+"Types");
//    		for (int i = 0; i < dates.size(); i++) {
//    			if (datetypes.get(i).equals("Bestandslaufzeit")) {
//    				logger.debug("found archival date: " + dates.get(i));
//    				currentGraph.put(UNITDATE, dates.get(i));
//    			} else {
//    				logger.debug("found other type of date: " + dates.get(i));
//    				currentGraph.put("otherFormsOfDate", dates.get(i));
//    			}
//    		}
//    		currentGraph.remove(UNITDATE+"Types"); // Why only when ..Type(s) is a list?
//    	}
	}

    @Override
	protected void extractIdentifier(Map<String, Object> currentGraph) {
		// NIOD has only relative IDs. The EAD ID should be the archdesc ID.
    	// We should concatenate all IDs from the top to the bottom to create a document wide unique ID.
    	//not all units have ids, and some have multiple, find the Bestandssignatur
		String currID = "", tempID = "";
    	if (eadid != null) {
    		logger.debug("Current ID at eadid check: "+ eadid);
			tempID += eadid + "_";
		}
    	if (currentGraph.containsKey("objectIdentifier")) {
			// replace current ID with concatenation of all IDs
			currID += (String) currentGraph.get("objectIdentifier");
			logger.debug("current ID: " + currID);
			tempID += depth > 0 ? ids[depth-1] + "_" + currID : currID;
			logger.debug("tempID: "+tempID);
			ids[depth] = tempID;
			logger.debug(ids[depth]);
			currentGraph.put("objectIdentifier", tempID);
//			ids[depth] = null;
			
//		    if (currentGraph.get("objectIdentifier") instanceof List) {
//		        logger.debug("class of identifier: " + currentGraph.get("objectIdentifier").getClass());
//		        ArrayList<String> identifiers = (ArrayList<String>) currentGraph.get("objectIdentifier");
//		        ArrayList<String> identifiertypes = (ArrayList<String>) currentGraph.get("objectIdentifierType");
//		        for (int i = 0; i < identifiers.size(); i++) {
//		            if (identifiertypes.get(i).equals("Bestandssignatur")) {
//		                logger.debug("found official id: " + identifiers.get(i));
//		                currentGraph.put("objectIdentifier", identifiers.get(i));
//		            }else {
//		                logger.debug("found other form of identifier: " + identifiers.get(i));
//		                currentGraph.put("arta", identifiers.get(i));
//		            }
//		        }
//		        currentGraph.remove("objectIdentifierType");
//		    }
		} else {
		    logger.error("no unitid found, setting " + ++niodcount);
		    currentGraph.put("objectIdentifier", eadid+"_niodID"+niodcount);
		    
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
