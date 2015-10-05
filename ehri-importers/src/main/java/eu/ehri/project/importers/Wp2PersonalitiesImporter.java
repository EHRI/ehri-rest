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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.importers.util.Helpers;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Importer of historical agents described in WP2.
 * before importing the file: delete the columns with the reordering of the first and last name
 * add a column 'id' with a unique identifier, prefixed with EHRI-Personalities or some such.
 *
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public abstract class Wp2PersonalitiesImporter extends MapImporter {

    private final XmlImportProperties p = new XmlImportProperties("wp2personalities.properties");

    private static final Logger logger = LoggerFactory.getLogger(Wp2PersonalitiesImporter.class);

    public Wp2PersonalitiesImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope,
            ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError {
        Bundle unit = new Bundle(EntityClass.HISTORICAL_AGENT, extractUnit(itemData));

        Bundle descBundle = new Bundle(EntityClass.HISTORICAL_AGENT_DESCRIPTION, extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));

        for (Map<String, Object> dpb : extractDates(itemData)) {
            descBundle = descBundle.withRelation(Ontology.ENTITY_HAS_DATE, new Bundle(EntityClass.DATE_PERIOD, dpb));
        }
        unit = unit.withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        BundleDAO persister = getPersister();
        Mutation<HistoricalAgent> mutation = persister.createOrUpdate(unit, HistoricalAgent.class);
        HistoricalAgent frame = mutation.getNode();

        if (!permissionScope.equals(SystemScope.getInstance())
                && mutation.created()) {
            manager.cast(permissionScope, AuthoritativeSet.class).addItem(frame);
            frame.setPermissionScope(permissionScope);
        }

        handleCallbacks(mutation);
        return frame;

    }

    public AccessibleEntity importItem(Map<String, Object> itemData, int depth) throws ValidationError {
        throw new UnsupportedOperationException("Not supported ever.");
    }

    private Map<String, Object> extractUnit(Map<String, Object> itemData) {
        //unit needs at least IDENTIFIER_KEY
        Map<String, Object> item = Maps.newHashMap();
        if (itemData.containsKey("id")) {
            item.put(Ontology.IDENTIFIER_KEY, itemData.get("id"));
        } else {
            logger.error("missing objectIdentifier");
        }
        return item;
    }


    private Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entityClass) {
        Map<String, Object> item = Maps.newHashMap();

        for (String key : itemData.keySet()) {
            if (!key.equals("id")) {
                if (!p.containsProperty(key)) {
                    Helpers.putPropertyInGraph(item, SaxXmlHandler.UNKNOWN + key, itemData.get(key).toString());
                } else {
                    Helpers.putPropertyInGraph(item, p.getProperty(key), itemData.get(key).toString());
                }
            }

        }
        //create all otherFormsOfName
        if (!item.containsKey("typeOfEntity")) {
            Helpers.putPropertyInGraph(item, "typeOfEntity", "person");
        }
        if (!item.containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
            Helpers.putPropertyInGraph(item, Ontology.LANGUAGE_OF_DESCRIPTION, "en");
        }
        return item;
    }


    /**
     * @param itemData the item data map
     * @return returns a List with Maps with DatePeriod.DATE_PERIOD_START_DATE and DatePeriod.DATE_PERIOD_END_DATE values
     */
    @Override
    public List<Map<String, Object>> extractDates(Map<String, Object> itemData) {

        List<Map<String, Object>> l = Lists.newArrayList();
        Map<String, Object> items = Maps.newHashMap();

        String start = (String) itemData.get("dateOfBirth");

        if (start != null && start.endsWith("00-00")) {
            start = start.substring(0, 4);
        }
        if (start != null) {
            items.put(Ontology.DATE_PERIOD_START_DATE, start);
            l.add(items);
        }
        return l;
    }
}
