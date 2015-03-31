/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.properties.XmlImportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Base handler for EAD, EAC and EAG files, based on a SAX reader.
 * This only contains a utility method.
 * 
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
            if (firstname instanceof String) {
                nameValue = firstname.toString();
            } else {
                Map<String, Object> nameMap = (Map) firstname;
                if(nameMap.get("namePart") instanceof String){
                  nameValue = nameMap.get("namePart").toString();  
                }else if(nameMap.get("namePart") instanceof List){
                    nameValue="";
                    for(Object p : (List)nameMap.get("namePart")){
                        nameValue += p.toString()+ " ";
                    }
                    
                }else{
                    nameValue = nameMap.get("namePart").toString();
                }
            }
//            nameValue = ((List) names).get(0).toString();
            for (int i = 1; i < ((List) names).size(); i++) {
                Map<String, Object> m = (Map) ((List) names).get(i);
                logger.debug("other name: " + m.get("namePart"));
                putPropertyInCurrentGraph("otherFormsOfName", m.get("namePart").toString());
            }
        } else {
            logger.warn("no " + Ontology.NAME_KEY + " found");
            nameValue = "no title";
        }
        return nameValue.trim();
    }
}
