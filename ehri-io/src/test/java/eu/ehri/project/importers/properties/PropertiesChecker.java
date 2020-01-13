/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.properties;

import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.models.EntityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;


public class PropertiesChecker {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesChecker.class);
    private final NodeProperties allowed;

    public PropertiesChecker(NodeProperties allowedProperties) {
        allowed = allowedProperties;
    }

    /*
     * Checks whether these actualProperties adhere to the allowedProperties
     * the actualProperties can contain less than the allowedProperties,
     * but must at least contain the required ones and it will log the
     * superfluous ones
     *
     * @param actualProperties
     * @return returns true if it contains the required ones and no new ones
     */
    public boolean check(ImportProperties actualProperties, EntityClass nodeEntity) {
        String node = nodeEntity.getName();
        boolean testResult = true;
        if (allowed.getHandlerProperties(node) == null) {
            logger.error("no properties allowed for {}", node);
            return false;
        }
        if (allowed.getRequiredProperties(node) != null) {
            for (String required : allowed.getRequiredProperties(node)) {
                if (!actualProperties.containsPropertyValue(required)) {
                    logger.error("{} should contain required {}", node, required);
                    testResult = false;
                }
            }
        }
        Set<String> handler = allowed.getHandlerProperties(node);
        for (String property : actualProperties.getAllNonAttributeValues()) {
            if (property.isEmpty() || property.equals("")) {
                logger.error("property file contains empty right-hand side");
                testResult = false;
            }
            if (!handler.contains(property)) {
                if (nodeEntity.equals(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION) &&
                        property.endsWith(EadImporter.ACCESS_POINT)) {
                    logger.debug(property.substring(0, property.indexOf(EadImporter.ACCESS_POINT) + 6));
                } else {
                    logger.warn( "{} should NOT contain {}", node, property);
                }
            }
        }
        return testResult;
    }
}
