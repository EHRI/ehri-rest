/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            nameValue = ((List) names).get(0).toString();
            for (int i = 1; i < ((List) names).size(); i++) {
                putPropertyInCurrentGraph("otherFormsOfName", ((List) names).get(i).toString());
            }
        } else {
            logger.warn("no " + Ontology.NAME_KEY + " found");
            nameValue = "no title";
        }
        return nameValue;
    }
}
