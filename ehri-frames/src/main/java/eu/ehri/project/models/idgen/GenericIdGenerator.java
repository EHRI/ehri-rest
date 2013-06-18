package eu.ehri.project.models.idgen;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.Bundle;

/**
 * Generates a generic ID for tertiary node types.
 * 
 * @author michaelb
 * 
 */
public enum GenericIdGenerator implements IdGenerator {
    
    INSTANCE;

    public void handleIdCollision(EntityClass type, PermissionScope scope,
            Bundle bundle) throws ValidationError {
        throw new RuntimeException(String.format("Index collision generating identifier for item type '%s' with data: '%s'",
                type, bundle));
    }


    /**
     * Generates a random string UUID.
     */
    public String generateId(EntityClass type, PermissionScope scope,
            Bundle bundle) {
        return generateId(type, Lists.<String>newArrayList(), bundle);
    }

    /**
     * Generates a random String.
     * @param type
     * @param scopeIds array of scope ids
     * @param bundle
     * @return
     */
    public String generateId(EntityClass type, List<String> scopeIds, Bundle bundle) {
        return UUID.randomUUID().toString();
    }
}
