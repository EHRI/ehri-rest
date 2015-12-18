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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.util.Helpers;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.AuthoritativeItem;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Importer of authoritative items such as historical agents and concepts, loaded
 * from a CSV file.
 */
public class CsvAuthoritativeItemImporter extends MapImporter {

    private static final Logger logger = LoggerFactory.getLogger(CsvAuthoritativeItemImporter.class);

    public CsvAuthoritativeItemImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope, ImportLog log) {
        super(framedGraph, permissionScope, log);
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData) throws ValidationError {

        BundleDAO persister = getPersister();
        Bundle descBundle = new Bundle(EntityClass.HISTORICAL_AGENT_DESCRIPTION,
                extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));
        Bundle unit = new Bundle(EntityClass.HISTORICAL_AGENT, extractUnit(itemData))
                .withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        Mutation<AuthoritativeItem> mutation = persister.createOrUpdate(unit, AuthoritativeItem.class);
        AuthoritativeItem frame = mutation.getNode();

        if (!permissionScope.equals(SystemScope.getInstance()) && mutation.created()) {
            permissionScope.as(AuthoritativeSet.class).addItem(frame);
            frame.setPermissionScope(permissionScope);
        }

        handleCallbacks(mutation);
        return frame;
    }

    @Override
    public AccessibleEntity importItem(Map<String, Object> itemData, List<String> idPath) throws
            ValidationError {
        throw new UnsupportedOperationException("Not supported ever.");
    }

    protected Map<String, Object> extractUnit(Map<String, Object> itemData) {
        //unit needs at least IDENTIFIER_KEY
        Map<String, Object> item = Maps.newHashMap();
        if (itemData.containsKey("id")) {
            item.put(Ontology.IDENTIFIER_KEY, itemData.get("id"));
        } else {
            logger.error("missing objectIdentifier");
        }
        return item;
    }

    protected Map<String, Object> extractUnitDescription(Map<String, Object> itemData, EntityClass entityClass) {
        Map<String, Object> item = Maps.newHashMap();
        item.put(Ontology.CREATION_PROCESS, Description.CreationProcess.IMPORT.toString());

        Helpers.putPropertyInGraph(item, Ontology.NAME_KEY, itemData.get("name").toString());
        for (String key : itemData.keySet()) {
            if (!key.equals("id") && !key.equals("name")) {
                Helpers.putPropertyInGraph(item, key, itemData.get(key).toString());
            }
        }
        if (!item.containsKey("typeOfEntity")) {
            Helpers.putPropertyInGraph(item, "typeOfEntity", "subject");
        }
        if (!item.containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
            Helpers.putPropertyInGraph(item, Ontology.LANGUAGE_OF_DESCRIPTION, "en");
        }
        return item;
    }

    @Override
    public List<Map<String, Object>> extractDates(Map<String, Object> itemData) {

        List<Map<String, Object>> l = Lists.newArrayList();
        Map<String, Object> items = Maps.newHashMap();

        String end = (String) itemData.get("DateofdeathYYYY-MM-DD");
        String start = (String) itemData.get("DateofbirthYYYY-MM-DD");
        if (start != null && start.endsWith("00-00")) {
            start = start.substring(0, 4);
        }
        if (end != null && end.endsWith("00-00")) {
            end = end.substring(0, 4);
        }
        if (end != null || start != null) {
            if (start != null)
                items.put(Ontology.DATE_PERIOD_START_DATE, start);
            if (end != null)
                items.put(Ontology.DATE_PERIOD_END_DATE, end);
            l.add(items);
        }
        return l;
    }
}
