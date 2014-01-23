package eu.ehri.project.models.idgen;

import com.google.common.collect.ListMultimap;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;

import java.util.List;

import static eu.ehri.project.definitions.Ontology.IDENTIFIER_KEY;

/**
 * Generates an ID for nodes which represent IdentifiableEntities, where
 * The graph id is .
 * 
 * @author michaelb
 * 
 */
public enum IdentifiableEntityIdGenerator implements IdGenerator {

    INSTANCE;

    public ListMultimap<String,String> handleIdCollision(PermissionScope scope, Bundle bundle) {
        return IdGeneratorUtils.handleIdCollision(scope, IDENTIFIER_KEY, getIdBase(bundle));
    }


    /**
     * Uses the items identifier and its entity type to generate a (supposedly)
     * unique ID.
     */
    public String generateId(PermissionScope scope,
            Bundle bundle) {
        return IdGeneratorUtils.generateId(scope, bundle, getIdBase(bundle));
    }

    /**
     * Use an array of scope IDs and the bundle data to generate a unique
     * id within a given scope.
     *
     * @param scopeIds An array of scope ids
     * @param bundle The bundle
     * @return The calculated identifier
     */
    public String generateId(List<String> scopeIds, Bundle bundle) {
        return IdGeneratorUtils.generateId(scopeIds, bundle, getIdBase(bundle));
    }

    /**
     * Return the base data for the id, sans scoping.
     * @param bundle The entity's bundle.
     * @return The base id string.
     */
    public String getIdBase(Bundle bundle) {
        return (String)bundle.getDataValue(IDENTIFIER_KEY);
    }
}
