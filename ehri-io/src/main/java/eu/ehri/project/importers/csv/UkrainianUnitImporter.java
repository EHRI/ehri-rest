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

package eu.ehri.project.importers.csv;

import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.base.MapImporter;
import eu.ehri.project.importers.base.SaxXmlHandler;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.importers.util.Helpers;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Importer of Ukrainian units, which are encoded in a slightly different way
 * than other documentary units.
 */
public class UkrainianUnitImporter extends MapImporter {

    public static final String MULTIVALUE_SEP = ",,";
    private final XmlImportProperties p;
    private static final Logger logger = LoggerFactory.getLogger(UkrainianUnitImporter.class);

    public UkrainianUnitImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope,
            Actioner actioner, ImportLog log) {
        super(framedGraph, permissionScope, actioner, log);
        p = new XmlImportProperties("ukraine.properties");
    }

    @Override
    public Accessible importItem(Map<String, Object> itemData) throws ValidationError {

        BundleManager persister = new BundleManager(framedGraph, permissionScope.idPath());

        logger.debug("-----------------------------------");
        Bundle unit = Bundle.of(EntityClass.DOCUMENTARY_UNIT, extractUnit(itemData));
        Map<String, Object> unknowns = extractUnknownProperties(itemData);

        String lang = itemData.get("language_of_description").toString();
        if (lang.indexOf(", ") > 0) {
            String[] langs = lang.split(", ");
            for (String l : langs) {
                Bundle descBundle = Bundle.of(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION, extractUnitDescription(itemData, l));
                descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, Bundle.of(EntityClass.DATE_PERIOD, constructDateMap(itemData)));
                if (!unknowns.isEmpty()) {
                    descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, Bundle.of(EntityClass.UNKNOWN_PROPERTY, unknowns));
                }
                unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
            }
        } else {
            Bundle descBundle = Bundle.of(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION, extractUnitDescription(itemData, lang));
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, Bundle.of(EntityClass.DATE_PERIOD, constructDateMap(itemData)));
            if (!unknowns.isEmpty()) {
                descBundle = descBundle.withRelation(Ontology.HAS_UNKNOWN_PROPERTY, Bundle.of(EntityClass.UNKNOWN_PROPERTY, unknowns));
            }

            unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);
        }

        Mutation<DocumentaryUnit> mutation = persister
                .createOrUpdate(unit, DocumentaryUnit.class);
        DocumentaryUnit frame = mutation.getNode();
        if (!permissionScope.equals(SystemScope.getInstance())
                && mutation.created()) {
            frame.setRepository(framedGraph.frame(permissionScope.asVertex(), Repository.class));
            frame.setPermissionScope(permissionScope);
        }

        handleCallbacks(mutation);
        return frame;

    }

    private Map<String, Object> extractUnknownProperties(Map<String, Object> itemData) throws ValidationError {
        Map<String, Object> unknowns = Maps.newHashMap();
        for (String key : itemData.keySet()) {
            if (p.getProperty(key).equals("UNKNOWN")) {
                unknowns.put(key, itemData.get(key));
            }
        }
        return unknowns;
    }


    @Override
    public Accessible importItem(Map<String, Object> itemData, List<String> scopeIds) throws
            ValidationError {
        throw new UnsupportedOperationException("Not supported ever.");
    }

    private Map<String, Object> extractUnit(Map<String, Object> itemData) {
        //unit needs at least IDENTIFIER_KEY
        Map<String, Object> item = Maps.newHashMap();
        putIfNotNull(item, "scope", itemData.get(p.getFirstPropertyWithValue("scope")));
        putIfNotNull(item, "priority", itemData.get(p.getFirstPropertyWithValue("priority")));
        putIfNotNull(item, "copyrightStatus", itemData.get(p.getFirstPropertyWithValue("copyrightStatus")));
        if (itemData.containsKey("identifier")) {
            item.put(Ontology.IDENTIFIER_KEY, itemData.get("identifier"));
        } else {
            logger.error("missing identifier");
        }
        return item;
    }

    private void putIfNotNull(Map<String, Object> item, String key, Object value) {
        if (value != null && !value.toString().isEmpty()) {
            item.put(key, value);
        }
    }

    public Map<String, Object> constructDateMap(Map<String, Object> itemData) {
        Map<String, Object> item = Maps.newHashMap();
        String origDate = itemData.get("dates").toString();
        if (origDate.indexOf(MULTIVALUE_SEP) > 0) {
            String[] dates = itemData.get("dates").toString().split(MULTIVALUE_SEP);
            item.put(Ontology.DATE_PERIOD_START_DATE, dates[0]);
            item.put(Ontology.DATE_PERIOD_END_DATE, dates[1]);
        } else {
            item.put(Ontology.DATE_PERIOD_START_DATE, origDate);
            item.put(Ontology.DATE_PERIOD_END_DATE, origDate);
        }
        return item;
    }

    private Map<String, Object> extractUnitDescription(Map<String, Object> itemData, String language) {
        Map<String, Object> item = Maps.newHashMap();

        for (String key : itemData.keySet()) {
            if ((!key.equals("identifier")) &&
                    !(p.getProperty(key).equals("IGNORE")) &&
                    !(p.getProperty(key).equals("UNKNOWN")) &&
                    (!key.equals("dates")) &&
                    (!p.getProperty(key).equals("scope")) && //on the unit
                    (!p.getProperty(key).equals("copyrightStatus")) && // on the unit
                    (!p.getProperty(key).equals("priority")) && // on the unit
                    (!key.equals("language_of_description"))  //dealt with in importItem
                    ) {
                if (!p.containsProperty(key)) {
                    Helpers.putPropertyInGraph(item, SaxXmlHandler.UNKNOWN + key, itemData.get(key).toString());
                } else {
                    Object value = itemData.get(key);
                    // TODO: Check if the property is an allowedMultivalue one...
                    if (value.toString().contains(MULTIVALUE_SEP)) {
                        for (String v : value.toString().split(MULTIVALUE_SEP)) {
                            Helpers.putPropertyInGraph(item, p.getProperty(key), v);
                        }
                    } else {
                        Helpers.putPropertyInGraph(item, p.getProperty(key), value.toString());
                    }
                }
            }

        }
        //replace the language from the itemData with the one specified in the param
        Helpers.putPropertyInGraph(item, Ontology.LANGUAGE_OF_DESCRIPTION, language);
        return item;
    }
}
