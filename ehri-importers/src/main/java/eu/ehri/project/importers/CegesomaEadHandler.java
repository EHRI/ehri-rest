package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * makes use of icaatom.properties with format: part/of/path/=attribute
 *
 * @author linda
 */
public class CegesomaEadHandler extends EadHandler {

    private static int cegesomacount = 0;
    private static final Logger logger = LoggerFactory
            .getLogger(CegesomaEadHandler.class);
    

    public CegesomaEadHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("cegesoma.properties"));
        this.defaultLanguage = "nld";
    }

    /**
     * Picks the first identifier as the 'official' ID if there were multiple unitids in the EAD
     * and puts the others in the otherIdentifiers field.
     */
    @Override
    protected void extractIdentifier(Map<String, Object> currentGraph) {
    	// Is there an identifier?
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
            logger.error("no unitid found, setting " + getEadId() + "-" + ++cegesomacount);
            currentGraph.put("objectIdentifier", "cegesomaID-"+getEadId() + "-" +cegesomacount);
            
        }
    }

    /**
     * If there is no title, generate a generic one.
     * If there are multiple titles, use the first as 'the' title
     * and put the others in otherFormsOfName
     */
    @Override
    protected void extractTitle(Map<String, Object> currentGraph) {
    	if (!currentGraph.containsKey(Ontology.NAME_KEY)) {
            //finding some name for this unit:
            logger.error("Cegesoma node without name field: ");
            currentGraph.put(Ontology.NAME_KEY, "UNKNOWN title" );
        } else if (currentGraph.get(Ontology.NAME_KEY) instanceof List) {
            logger.debug("class of identifier: " + currentGraph.get(Ontology.NAME_KEY).getClass());
            ArrayList<String> names = (ArrayList<String>) currentGraph.get(Ontology.NAME_KEY);
            logger.debug("Using first name as official name: " + names.get(0));
            currentGraph.put(Ontology.NAME_KEY, names.get(0));
            
            for (int i = 1; i < names.size(); i++) {
                logger.debug("Adding other names as 'other form of name': " + names.get(i));
                currentGraph.put("otherFormsOfName", names.get(i));
            }
            currentGraph.remove(Ontology.NAME_KEY+"Type");
        }
	}
    
    /**
     * Cegesoma uses 'N' for Dutch and 'F' for French; we need to expand these 'codes'
     * to 'nld' and 'fre'.
     */
//    @Override
//    protected void useDefaultLanguage(Map<String, Object> currentGraph) {
//    	// TODO
//    }
    
    
    @Override
    protected void extractDate(Map<String, Object> currentGraph) {
    	// Cegesoma have multiple unitdates, but with different types, find the inclusive
        // there is a difference between Ontology.ENTITY_HAS_DATE and the .properties-defined term for unit dates
        final String UNITDATE = "unitDates";  
        
        if (!currentGraph.containsKey(UNITDATE)) {
        	// Log an error with the identifier (should have been set by now)
            // Add a generic date for this unit without date:
            logger.error("Cegesoma node without unitdate field: " + currentGraph.get(Ontology.IDENTIFIER_KEY));
            currentGraph.put(Ontology.ENTITY_HAS_DATE, "UNKNOWN date" );
        } else if(currentGraph.get(UNITDATE) instanceof List){
            logger.debug("class of dates: " + currentGraph.get(UNITDATE).getClass());
            ArrayList<String> dates = (ArrayList<String>) currentGraph.get(UNITDATE);
            ArrayList<String> datetypes = (ArrayList<String>) currentGraph.get(UNITDATE+"Types");
            for (int i = 0; i < dates.size(); i++) {
                if (datetypes.get(i).equals("inclusive")) {
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
    
    
}
