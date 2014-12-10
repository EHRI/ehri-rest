package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public abstract class EaHandler extends SaxXmlHandler {
    private static final Logger logger = LoggerFactory.getLogger(EaHandler.class);

    public EaHandler(AbstractImporter<Map<String, Object>> importer, XmlImportProperties properties) {
        super(importer, properties);
    }

    protected String chooseName(Object names) {
        String nameValue;
        if (names instanceof String) {
            nameValue = names.toString();
        } else if (names instanceof List) {
           Object firstname = ((List) names).get(0);
           if(firstname instanceof String){
            nameValue=firstname.toString();
        }else {
               Map<String, Object> nameMap = (Map)firstname;
               nameValue = nameMap.get("namePart").toString();
           }
//            nameValue = ((List) names).get(0).toString();
            for (int i = 1; i < ((List) names).size(); i++) {
                Map<String, Object> m = (Map)((List) names).get(i);
                logger.debug("other name: "+ m.get("namePart"));
                putPropertyInCurrentGraph("otherFormsOfName", m.get("namePart").toString());
            }
        } else {
            logger.warn("no " + Ontology.NAME_KEY + " found");
            nameValue = "no title";
        }
        return nameValue;
    }
}
