/**
 * 
 */
package eu.ehri.project.importers;

import eu.ehri.project.importers.properties.XmlImportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handler for importing EAD files converted from the USHMM Solr index file.
 * These files were converted using the solr2ead XSLT stylesheet.
 * @author Ben Companjen (http://github.com/bencomp)
 */
public class UshmmHandler extends EadHandler {

	private static final Logger logger = LoggerFactory.getLogger(UshmmHandler.class);

    // Default language to use in units without language
    protected String defaultLanguage = "en";

	private int ushmmcount = 0;
    
	/**
	 * @param importer
	 */
	public UshmmHandler(AbstractImporter<Map<String, Object>> importer) {
		super(importer, new XmlImportProperties("ushmm.properties"));
		
	}

	/**
	 * Handler specific code for extraction of unit IDs
	 * @param currentGraph
	 */
	@Override
	protected void extractIdentifier(Map<String, Object> currentGraph) {
		//not all units have ids, and some have multiple, find the "irn"
		if (currentGraph.containsKey("objectIdentifier")) {
			if (currentGraph.get("objectIdentifier") instanceof List) {
				logger.debug("class of identifier: " + currentGraph.get("objectIdentifier").getClass());
				ArrayList<String> identifiers = (ArrayList<String>) currentGraph.get("objectIdentifier");
				ArrayList<String> identifiertypes = (ArrayList<String>) currentGraph.get("objectIdentifierType");
				for (int i = 0; i < identifiers.size(); i++) {
					if (identifiertypes.get(i).equals("irn")) {
						logger.debug("found official id: " + identifiers.get(i));
						currentGraph.put("objectIdentifier", identifiers.get(i));
					}else {
						logger.debug("found other form of identifier: " + identifiers.get(i));
						addOtherIdentifier(currentGraph, identifiers.get(i));
						//currentGraph.put("otherIdentifiers", identifiers.get(i));
					}
				}
				currentGraph.remove("objectIdentifierType");
			}
		} else {
			logger.error("no unitid found, setting " + ++ushmmcount);
			currentGraph.put("objectIdentifier", "ushmmID"+ushmmcount);

		}
	}

}
