package eu.ehri.project.models.idgen;

import java.util.Map;

import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;

/**
 * Generates an ID for nodes which represent AccessibleEntities.
 * 
 * @author michaelb
 * 
 */
public enum AccessibleEntityIdGenerator implements IdGenerator {
    
    INSTANCE;

    /**
     * Uses the items identifier and its entity type to generate a (supposedly)
     * unique ID.
     */
    public String generateId(EntityClass type, PermissionScope scope,
            Map<String, Object> data) throws IdGenerationError {
        return type.getAbbreviation() + SEPERATOR
                + data.get(AccessibleEntity.IDENTIFIER_KEY);
    }

}
