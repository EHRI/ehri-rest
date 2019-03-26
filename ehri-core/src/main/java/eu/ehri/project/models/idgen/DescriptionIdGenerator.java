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

package eu.ehri.project.models.idgen;

import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.Slugify;

import java.util.Collection;
import static eu.ehri.project.definitions.Ontology.IDENTIFIER_KEY;
import static eu.ehri.project.definitions.Ontology.LANGUAGE_OF_DESCRIPTION;

/**
 * Generates an ID for nodes which represent Descriptions, where
 * the graph id is a combination of the parent scopes, plus the description
 * language code, plus an optional description identifier (say, 'alt').
 */
public enum DescriptionIdGenerator implements IdGenerator {

    INSTANCE;

    public static final String DESCRIPTION_SEPARATOR = ".";

    private static final Joiner idJoiner = Joiner
            .on(IdGeneratorUtils.HIERARCHY_SEPARATOR).skipNulls();

    public static final Joiner descriptionJoiner = Joiner.on(DESCRIPTION_SEPARATOR);

    public ListMultimap<String,String> handleIdCollision(Collection<String> scopeIds, Bundle bundle) {
        return IdGeneratorUtils.handleIdCollision(scopeIds,
                Lists.newArrayList(LANGUAGE_OF_DESCRIPTION, IDENTIFIER_KEY),
                getIdBase(bundle));
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
        return descriptionJoiner.join(IdGeneratorUtils.joinPath(scopeIds), getIdBase(bundle));
    }

    /**
     * Return the base data for the id, sans scoping.
     * If the bundle contains a language of description, it comes first.
     * If the bundle has a non-empty identifier, it is used too.
     * @param bundle The entity's bundle.
     * @return The base id string, consisting of language code and/or identifier.
     */
    public String getIdBase(Bundle bundle) {
        String lang = bundle.getDataValue(LANGUAGE_OF_DESCRIPTION);
        String ident = bundle.getDataValue(IDENTIFIER_KEY);
        String identSlug = ident != null
            ? Slugify.slugify(ident, IdGeneratorUtils.SLUG_REPLACE)
            : null;
        if (identSlug != null && identSlug.trim().isEmpty()) {
            identSlug = null;
        }
        return idJoiner.join(lang, identSlug);
    }
}
