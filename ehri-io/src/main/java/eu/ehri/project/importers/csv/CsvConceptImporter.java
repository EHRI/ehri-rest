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
package eu.ehri.project.importers.csv;

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.models.cvoc.Vocabulary;
import eu.ehri.project.persistence.Bundle;

import java.util.Map;

import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Mutation;

/**
 * Importer of concepts loaded from a CSV file.
 */
public class CsvConceptImporter extends CsvAuthoritativeItemImporter {

    public CsvConceptImporter(FramedGraph<?> framedGraph, PermissionScope permissionScope,
            Actioner actioner, ImportLog log) {
        super(framedGraph, permissionScope, actioner, log);
    }

    @Override
    public Accessible importItem(Map<String, Object> itemData) throws ValidationError {

        BundleManager persister = getPersister();
        Bundle descBundle = new Bundle(EntityClass.CVOC_CONCEPT_DESCRIPTION,
                extractUnitDescription(itemData, EntityClass.CVOC_CONCEPT_DESCRIPTION));
        Bundle unit = new Bundle(EntityClass.CVOC_CONCEPT, extractUnit(itemData))
                .withRelation(Ontology.DESCRIPTION_FOR_ENTITY, descBundle);

        Mutation<Concept> mutation = persister.createOrUpdate(unit, Concept.class);
        Concept frame = mutation.getNode();

        if (!permissionScope.equals(SystemScope.getInstance()) && mutation.created()) {
            permissionScope.as(Vocabulary.class).addItem(frame);
            frame.setPermissionScope(permissionScope);
        }

        handleCallbacks(mutation);
        return frame;
    }
}
