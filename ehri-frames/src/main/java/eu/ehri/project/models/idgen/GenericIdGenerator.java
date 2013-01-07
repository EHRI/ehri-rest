package eu.ehri.project.models.idgen;

import java.util.Map;
import java.util.UUID;

import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.PermissionScope;

/**
 * Generates a generic ID for tertiary node types.
 * 
 * @author michaelb
 * 
 */
public enum GenericIdGenerator implements IdGenerator {
    
    INSTANCE;

    /**
     * Generates a random string UUID.
     */
    public String generateId(EntityClass type, PermissionScope scope,
            Map<String, Object> data) throws IdGenerationError {
        return type.getAbbreviation() + SEPERATOR + UUID.randomUUID();
    }
}
