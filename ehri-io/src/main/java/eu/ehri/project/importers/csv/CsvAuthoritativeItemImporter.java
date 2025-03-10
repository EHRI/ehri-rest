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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.csv;

import com.google.common.collect.Maps;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.ImportOptions;
import eu.ehri.project.importers.base.AbstractImporter;
import eu.ehri.project.importers.util.ImportHelpers;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.AuthoritativeItem;
import eu.ehri.project.models.cvoc.AuthoritativeSet;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Importer of authoritative items such as historical agents and concepts, loaded
 * from a CSV file.
 */
public class CsvAuthoritativeItemImporter extends AbstractImporter<Map<String, Object>, AuthoritativeItem> {

    private static final Logger logger = LoggerFactory.getLogger(CsvAuthoritativeItemImporter.class);

    public CsvAuthoritativeItemImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope,
                                        Actioner actioner, ImportOptions options, ImportLog log) {
        super(framedGraph, permissionScope, actioner, options, log);
    }

    @Override
    public AuthoritativeItem importItem(Map<String, Object> itemData) throws ValidationError {

        BundleManager persister = getPersister();
        Bundle descBundle = Bundle.of(EntityClass.HISTORICAL_AGENT_DESCRIPTION,
                extractUnitDescription(itemData, EntityClass.HISTORICAL_AGENT_DESCRIPTION));
        Bundle unit = Bundle.of(EntityClass.HISTORICAL_AGENT, extractUnit(itemData))
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
    public AuthoritativeItem importItem(Map<String, Object> itemData, List<String> idPath) throws
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

        ImportHelpers.putPropertyInGraph(item, Ontology.NAME_KEY, itemData.get("name").toString());
        for (String key : itemData.keySet()) {
            if (!key.equals("id") && !key.equals("name")) {
                ImportHelpers.putPropertyInGraph(item, key, itemData.get(key).toString());
            }
        }
        if (!item.containsKey("typeOfEntity")) {
            ImportHelpers.putPropertyInGraph(item, "typeOfEntity", "subject");
        }
        if (!item.containsKey(Ontology.LANGUAGE_OF_DESCRIPTION)) {
            ImportHelpers.putPropertyInGraph(item, Ontology.LANGUAGE_OF_DESCRIPTION, "eng");
        }
        return item;
    }
}
