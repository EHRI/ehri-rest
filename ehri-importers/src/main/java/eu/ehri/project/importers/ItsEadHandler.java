package eu.ehri.project.importers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.XMLPropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
<<<<<<< HEAD
import org.xml.sax.SAXException;
=======
>>>>>>> newmike/importer_merge

import eu.ehri.project.importers.properties.XmlImportProperties;

public class ItsEadHandler extends EadHandler {

	private static final Logger logger = LoggerFactory.getLogger(ItsEadHandler.class);

    // Default language to use in units without language
    protected String defaultLanguage = "de";

	private int itscount = 0;
<<<<<<< HEAD
	private String archID;
=======
>>>>>>> newmike/importer_merge
	
	public ItsEadHandler(AbstractImporter<Map<String, Object>> importer) {
		super(importer, new XmlImportProperties("its.properties"));
	}

<<<<<<< HEAD
	 @Override
	 public void endElement(String uri, String localName, String qName) throws SAXException {
		 //the child closes, add the new DocUnit to the list, establish some relations
		 super.endElement(uri, localName, qName);
		 if (qName.equals("eadid")) {
			 archID = (String) currentGraphPath.peek().get("eadIdentifier");
			 logger.debug("archID set: " + archID);
		 }
//		 else if (qName.equals("unitid")) {
//			 String uid = (String) currentGraphPath.peek().get("objectIdentifier");
//			 logger.debug("unitid at depth " + depth +": " + uid);
//		 }
	 }
	 
=======
	
>>>>>>> newmike/importer_merge
	/**
	 * Handler specific code for extraction of unit IDs
	 * @param currentGraph
	 */
	@Override
	protected void extractIdentifier(Map<String, Object> currentGraph) {
		//not all units have ids, and some have multiple, find the "bestellnummer"
		if (currentGraph.containsKey("objectIdentifier")) {
			if (currentGraph.get("objectIdentifier") instanceof List) {
				logger.debug("class of identifier: " + currentGraph.get("objectIdentifier").getClass());
				ArrayList<String> identifiers = (ArrayList<String>) currentGraph.get("objectIdentifier");
				ArrayList<String> identifiertypes = (ArrayList<String>) currentGraph.get("objectIdentifierType");
				for (int i = 0; i < identifiers.size(); i++) {
					if (identifiertypes.get(i).equals("bestellnummer")) {
						logger.debug("found official id: " + identifiers.get(i));
						currentGraph.put("objectIdentifier", identifiers.get(i));
					} else {
						logger.debug("found other form of identifier: " + identifiers.get(i));
						currentGraph.put("arta", identifiers.get(i));
					}
				}
				currentGraph.remove("objectIdentifierType");
			}
		} else {
			logger.error("no unitid found, setting " + ++itscount);
<<<<<<< HEAD
			currentGraph.put("objectIdentifier", this.archID+"-itsID"+itscount);
=======
			currentGraph.put("objectIdentifier", "itsID"+itscount);
>>>>>>> newmike/importer_merge

		}
	}

}
