package eu.ehri.project.importers.properties;

import eu.ehri.project.importers.EadImporter;
import eu.ehri.project.models.EntityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class PropertiesChecker {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesChecker.class);
    private NodeProperties allowed;

    public PropertiesChecker(NodeProperties allowedproperties) {
        allowed = allowedproperties;
    }

    /*
     * checks whether these actualproperties adhere to the allowedproperties
     * the actualproperties can contain less than the allowedproperties, but must at least contain the required ones 
     * and it will log the superfluous ones, so 
     * @param actualproperties
     * @return returns true if it contains the required ones and no new ones
     */
    public boolean check(ImportProperties actualproperties, EntityClass nodeEntity) {
        String node = nodeEntity.getName();
        boolean testresult = true;
        if (allowed.getHandlerProperties(node) == null) {
            logger.error("no properties allowed for " + node);
            return false;
        }
        if (allowed.getRequiredProperties(node) != null) {
            for (String required : allowed.getRequiredProperties(node)) {
                if (!actualproperties.containsPropertyValue(required)) {
                    logger.error(node + " should contain required " + required);
                    testresult = false;
                }
            }
        }
        Set<String> handler = allowed.getHandlerProperties(node);
        for (String property : actualproperties.getAllNonAttributeValues()) {
            if(property.isEmpty() || property.equals("")){
                logger.error("property file contains empty right-hand side");
                testresult=false;
            }
            if (!handler.contains(property)) {
                if(nodeEntity.equals(EntityClass.DOCUMENT_DESCRIPTION) &&
                    property.endsWith(EadImporter.ACCESS_POINT)){
                    logger.debug(property.substring(0, property.indexOf(EadImporter.ACCESS_POINT)+6));
                 
                }else{
                    logger.warn(node + " should NOT contain " + property);
                }
//                testresult = false;
            }
        }
        return testresult;
    }
}
