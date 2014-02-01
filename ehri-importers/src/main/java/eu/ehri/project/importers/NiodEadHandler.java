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
	}

    @Override
	protected void extractIdentifier(Map<String, Object> currentGraph) {
		// NIOD has only relative IDs. The EAD ID should be the archdesc ID.
        if (currentGraph.containsKey("objectIdentifier")) {
            // Multiple identifiers?
            if (currentGraph.get("objectIdentifier") instanceof List) {
                logger.debug("class of identifier: " + currentGraph.get("objectIdentifier").getClass());
                ArrayList<String> identifiers = (ArrayList<String>) currentGraph.get("objectIdentifier");
                logger.debug("Using first ID as official id: " + identifiers.get(0));
                currentGraph.put("objectIdentifier", identifiers.get(0));
                for (int i = 1; i < identifiers.size(); i++) {
                    logger.debug("Adding remaining IDs as other identifier: " + identifiers.get(i));
                    addOtherIdentifier(currentGraph, identifiers.get(i));
                }
                currentGraph.remove("objectIdentifierType");
            }
        } else {
            // Generate an identifier if there is none
            currentGraph.put("objectIdentifier", eadid);
        }
	}
}
