package eu.ehri.project.models.idgen;

import java.util.Map;
import java.util.UUID;

import eu.ehri.project.exceptions.IdGenerationError;
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
        return UUID.randomUUID().toString();
    }
}
