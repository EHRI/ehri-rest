package eu.ehri.project.models.idgen;

import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import eu.ehri.project.persistence.Bundle;


import static eu.ehri.project.definitions.Ontology.IDENTIFIER_KEY;
import static eu.ehri.project.definitions.Ontology.LANGUAGE_OF_DESCRIPTION;

/**
 * Generates an ID for nodes which represent Descriptions, where
 * The graph id is a combination of the parent scopes, plus the description
 * language code, plus an optional description identifier (say, 'alt').
 * 
 * @author michaelb
 * 
 */
public enum DescriptionIdGenerator implements IdGenerator {

    INSTANCE;

    public ListMultimap<String,String> handleIdCollision(Iterable<String> scopeIds, Bundle bundle) {
        return IdGeneratorUtils.handleIdCollision(scopeIds, LANGUAGE_OF_DESCRIPTION,
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
    public String generateId(Iterable<String> scopeIds, Bundle bundle) {
        return IdGeneratorUtils.generateId(scopeIds, bundle, getIdBase(bundle));
    }

    /**
     * Return the base data for the id, sans scoping.
     * @param bundle The entity's bundle.
     * @return The base id string.
     */
    public String getIdBase(Bundle bundle) {
        String lang = (String) bundle.getDataValue(LANGUAGE_OF_DESCRIPTION);
        String ident = (String) bundle.getDataValue(IDENTIFIER_KEY);
        if (ident != null && ident.trim().equals("")) {
            ident = null;
        }
        return Joiner.on(IdGeneratorUtils.SEPARATOR)
                .skipNulls().join(lang, ident);
    }
}
