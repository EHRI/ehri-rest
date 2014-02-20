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

    @SuppressWarnings("unchecked")
    public BundesarchiveEadHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("bundesarchive.properties"));
        defaultLanguage = "deu";
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
    		currentGraph.remove(UNITDATE+"Types");
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
    				} else {
    					logger.debug("found other form of identifier: " + identifiers.get(i));
    					addOtherIdentifier(currentGraph, identifiers.get(i));
    				}
    			}
    			currentGraph.remove("objectIdentifierType");
    		}
    	} else {
    		logger.error("no unitid found, setting " + ++bundesarchivecount);
    		currentGraph.put("objectIdentifier", "" + bundesarchivecount);

    	}
    }

}
