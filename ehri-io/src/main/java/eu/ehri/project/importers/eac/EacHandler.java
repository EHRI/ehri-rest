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

package eu.ehri.project.importers.eac;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.base.AbstractImporter;
import eu.ehri.project.importers.base.SaxXmlHandler;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.UnknownProperty;
import eu.ehri.project.models.base.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.util.List;
import java.util.Map;

import static eu.ehri.project.importers.base.SaxXmlImporter.MAINTENANCE_EVENT;


/**
 * Handler of EAC-CPF files. Creates a {@link Map} for the {@link EacImporter} to
 * import.
 * Makes use of eac.properties with format: part/of/path/=attribute
 */
public class EacHandler extends SaxXmlHandler {

    private final ImmutableMap<String, Class<? extends Entity>> possibleSubnodes = ImmutableMap
            .<String, Class<? extends Entity>>builder()
            .put(MAINTENANCE_EVENT, MaintenanceEvent.class)
            .put("relation", Annotation.class)
            .put("book", Annotation.class)
            .put("bookentry", Annotation.class)
            .put("accessPoint", Annotation.class)
            .put("name", UnknownProperty.class)
            .build();

    private static final Logger logger = LoggerFactory.getLogger(EacHandler.class);

    public EacHandler(AbstractImporter<Map<String, Object>> importer) {
        super(importer, new XmlImportProperties("eac.properties"));
    }

    public EacHandler(AbstractImporter<Map<String, Object>> importer, XmlImportProperties properties) {
        super(importer, properties);
    }

    @Override
    protected boolean needToCreateSubNode(String key) {
        return possibleSubnodes.containsKey(getImportantPath(currentPath));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //if a subnode is ended, add it to the super-supergraph
        super.endElement(uri, localName, qName);
        if (needToCreateSubNode(getImportantPath(currentPath))) {
            Map<String, Object> currentGraph = currentGraphPath.pop();
            putSubGraphInCurrentGraph(getImportantPath(currentPath), currentGraph);
            depth--;
        }

        currentPath.pop();

        //an EAC file consists of only 1 element, so if we're back at the root, we're done
        if (currentPath.isEmpty()) {
            try {
                logger.debug("depth close " + depth + " " + qName);
                //TODO: add any mandatory fields not yet there:
                if (!currentGraphPath.peek().containsKey(OBJECT_IDENTIFIER)) {
                    putPropertyInCurrentGraph(OBJECT_IDENTIFIER, "id");
                }

                //TODO: name can have only 1 value, others are otherFormsOfName
                if (currentGraphPath.peek().containsKey(Ontology.NAME_KEY)) {
                    String name = chooseName(currentGraphPath.peek().get(Ontology.NAME_KEY));
                    overwritePropertyInCurrentGraph(Ontology.NAME_KEY, name);
                }
                if (!currentGraphPath.peek().containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
                    logger.debug("no languageCode found");
                    putPropertyInCurrentGraph(Ontology.LANGUAGE_OF_DESCRIPTION, "eng");
                }

                importer.importItem(currentGraphPath.pop(), Lists.<String>newArrayList());

            } catch (ValidationError ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    private String chooseName(Object names) {
        String nameValue;
        if (names instanceof String) {
            nameValue = names.toString();
        } else if (names instanceof List) {
            Object firstName = ((List) names).get(0);
            if (firstName instanceof String) {
                nameValue = firstName.toString();
            } else {
                Map nameMap = (Map) firstName;
                if (nameMap.get("namePart") instanceof String) {
                    nameValue = nameMap.get("namePart").toString();
                } else if (nameMap.get("namePart") instanceof List) {
                    nameValue = "";
                    for (Object p : (List) nameMap.get("namePart")) {
                        nameValue += p + " ";
                    }

                } else {
                    nameValue = nameMap.get("namePart").toString();
                }
            }

            for (int i = 1; i < ((List) names).size(); i++) {
                Map m = (Map) ((List) names).get(i);
                logger.debug("other name: {}", m.get("namePart"));
                putPropertyInCurrentGraph("otherFormsOfName", m.get("namePart").toString());
            }
        } else {
            logger.warn("no name found");
            nameValue = "no title";
        }
        return nameValue.trim();
    }
}
