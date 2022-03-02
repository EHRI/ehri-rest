/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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
import com.google.common.collect.Sets;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.base.SaxXmlHandler;
import eu.ehri.project.importers.util.ImportHelpers;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.MaintenanceEvent;
import eu.ehri.project.models.UnknownProperty;
import eu.ehri.project.models.base.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Handler of EAC-CPF files. Creates a {@link Map} for the {@link EacImporter} to
 * import.
 * Makes use of eac.properties with format: part/of/path/=attribute
 */
public class EacHandler extends SaxXmlHandler {

    // Artificial subnodes
    private static final String NAME_ENTRY = "NameEntry";

    private final ImmutableMap<String, Class<? extends Entity>> possibleSubNodes = ImmutableMap
            .<String, Class<? extends Entity>>builder()
            .put(Entities.MAINTENANCE_EVENT, MaintenanceEvent.class)
            .put(Entities.ACCESS_POINT, AccessPoint.class)
            .put(Entities.DATE_PERIOD, DatePeriod.class)
            .put(NAME_ENTRY, UnknownProperty.class)
            .build();

    private static final Logger logger = LoggerFactory.getLogger(EacHandler.class);

    public EacHandler(ItemImporter<Map<String, Object>, ?> importer, ImportOptions options) {
        super(importer, options);
    }

    @Override
    protected boolean needToCreateSubNode(String key) {
        return possibleSubNodes.containsKey(getMappedProperty(currentPath));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //if a subnode is ended, add it to the super-supergraph
        super.endElement(uri, localName, qName);
        if (needToCreateSubNode(getMappedProperty(currentPath))) {
            Map<String, Object> currentGraph = currentGraphPath.pop();
            putSubGraphInCurrentGraph(getMappedProperty(currentPath), currentGraph);
            depth--;
        }

        currentPath.pop();

        //an EAC file consists of only 1 element, so if we're back at the root, we're done
        if (currentPath.isEmpty()) {
            try {
                logger.trace("depth close {} {}", depth, qName);
                //TODO: add any mandatory fields not yet there:
                if (!currentGraphPath.peek().containsKey(ImportHelpers.OBJECT_IDENTIFIER)) {
                    putPropertyInCurrentGraph(ImportHelpers.OBJECT_IDENTIFIER, "id");
                }

                //TODO: name can have only 1 value, others are otherFormsOfName
                if (currentGraphPath.peek().containsKey(NAME_ENTRY)) {
                    String name = chooseName(currentGraphPath.peek().get(NAME_ENTRY));
                    overwritePropertyInCurrentGraph(Ontology.NAME_KEY, name);
                }
                if (!currentGraphPath.peek().containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
                    logger.trace("no {} found", Ontology.LANGUAGE_OF_DESCRIPTION);
                    putPropertyInCurrentGraph(Ontology.LANGUAGE_OF_DESCRIPTION, langCode);
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
            Object firstName = ((List<?>) names).get(0);
            if (firstName instanceof String) {
                nameValue = firstName.toString();
            } else {
                Map<?,?> nameMap = (Map<?,?>) firstName;
                if (nameMap.get("namePart") instanceof String) {
                    nameValue = nameMap.get("namePart").toString();
                } else if (nameMap.get("namePart") instanceof List) {
                    nameValue = "";
                    for (Object p : (List<?>) nameMap.get("namePart")) {
                        nameValue += p + " ";
                    }

                } else {
                    nameValue = nameMap.get("namePart").toString();
                }
            }

            Set<String> otherNames = Sets.newHashSet();
            for (int i = 1; i < ((List<?>) names).size(); i++) {
                Map<?,?> m = (Map<?,?>) ((List<?>) names).get(i);
                Object namePart = m.get("namePart");
                if (namePart != null && !namePart.toString().trim().equalsIgnoreCase(nameValue)) {
                    otherNames.add(namePart.toString().trim());
                    logger.trace("other name: {}", namePart);
                }
            }
            otherNames.forEach(n -> putPropertyInCurrentGraph("otherFormsOfName", n));
        } else {
            logger.warn("no {} found", Ontology.NAME_KEY);
            nameValue = "no title";
        }
        return nameValue.trim();
    }
}
