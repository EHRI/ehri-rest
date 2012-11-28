package eu.ehri.project.models.idgen;


import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.models.base.PermissionScope;

import java.util.Map;
import java.util.UUID;

/**
 * Generates a generic ID for tertiary node types.
 * 
 * @author michaelb
 * 
 */
public class GenericIdGenerator implements IdGenerator {

    /**
     * Generates a random string UUID.
     */
    public String generateId(String entityTypePrefix, PermissionScope scope, Map<String,Object> data) throws IdGenerationError {
        return entityTypePrefix + SEPERATOR + UUID.randomUUID();
    }
}
