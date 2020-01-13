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

package eu.ehri.project.models.idgen;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import eu.ehri.project.persistence.Bundle;

import java.util.Collection;

import static eu.ehri.project.definitions.Ontology.IDENTIFIER_KEY;

/**
 * Generates an ID for nodes which represent IdentifiableEntities, where
 * The graph id is derived from a property called
 * {@value eu.ehri.project.definitions.Ontology#IDENTIFIER_KEY}.
 */
public enum IdentifiableEntityIdGenerator implements IdGenerator {

    INSTANCE;

    public ListMultimap<String,String> handleIdCollision(Collection<String> scopeIds, Bundle bundle) {
        return IdGeneratorUtils.handleIdCollision(scopeIds, Lists.newArrayList(IDENTIFIER_KEY), getIdBase(bundle));
    }


    /**
     * Use an array of scope IDs and the bundle data to generate a unique
     * id within a given scope.
     *
     * @param scopeIds An array of scope ids
     * @param bundle The bundle
     * @return The calculated identifier
     */
    public String generateId(Collection<String> scopeIds, Bundle bundle) {
        return IdGeneratorUtils.generateId(scopeIds, bundle, getIdBase(bundle));
    }

    /**
     * Return the base data for the id, sans scoping.
     * @param bundle The entity's bundle.
     * @return The base id string.
     */
    public String getIdBase(Bundle bundle) {
        return bundle.getDataValue(IDENTIFIER_KEY);
    }
}
