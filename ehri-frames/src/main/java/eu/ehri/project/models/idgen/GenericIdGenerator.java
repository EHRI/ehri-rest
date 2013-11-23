package eu.ehri.project.models.idgen;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;

import java.util.List;
import java.util.UUID;

/**
 * Generates a generic ID for tertiary node types.
 *
 * @author michaelb
 */
public enum GenericIdGenerator implements IdGenerator {

    INSTANCE;

    public ListMultimap<String, String> handleIdCollision(EntityClass type, PermissionScope scope,
            Bundle bundle) {
        throw new RuntimeException(String.format("Index collision generating identifier for item type '%s' with data: '%s'",
                type, bundle));
    }


    /**
     * Generates a random string UUID.
     *
     * @param type   The type of the entity
     * @param scope  The entity's parent scope
     * @param bundle The entity's bundle data
     * @return A generated ID string
     */
    public String generateId(EntityClass type, PermissionScope scope,
            Bundle bundle) {
        return generateId(type, Lists.<String>newArrayList(), bundle);
    }

    /**
     * Generates a random String.
     *
     * @param type     The entity's type
     * @param scopeIds array of scope ids
     * @param bundle   The entity's bundle data
     * @return A generated ID string
     */
    public String generateId(EntityClass type, List<String> scopeIds, Bundle bundle) {
        return getIdBase(bundle);
    }

    /**
     * Return the base data for the id, sans scoping.
     * @param bundle The entity's bundle.
     * @return The base id string.
     */
    public String getIdBase(Bundle bundle) {
        return UUID.randomUUID().toString();
    }
}
