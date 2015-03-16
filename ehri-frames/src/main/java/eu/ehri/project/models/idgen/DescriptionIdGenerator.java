package eu.ehri.project.models.idgen;

import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.utils.Slugify;

import java.util.Collection;
import static eu.ehri.project.definitions.Ontology.IDENTIFIER_KEY;
import static eu.ehri.project.definitions.Ontology.LANGUAGE_OF_DESCRIPTION;

/**
 * Generates an ID for nodes which represent Descriptions, where
 * the graph id is a combination of the parent scopes, plus the description
 * language code, plus an optional description identifier (say, 'alt').
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 */
public enum DescriptionIdGenerator implements IdGenerator {

    INSTANCE;

    public static final String DESCRIPTION_SEPARATOR = ".";

    private static final Joiner idJoiner = Joiner
            .on(IdGeneratorUtils.HIERARCHY_SEPARATOR).skipNulls();

    public static final Joiner descriptionJoiner = Joiner.on(DESCRIPTION_SEPARATOR);

    public ListMultimap<String,String> handleIdCollision(Collection<String> scopeIds, Bundle bundle) {
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
