package eu.ehri.project.models.idgen;

import java.util.Map;


import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.models.base.PermissionScope;

/**
 * Generate an ID given an entity type and a vertex.
 * 
 * @author michaelb
 * 
 */
public interface IdGenerator {

    /**
     * Separate or ID components.
     */
    public static final String SEPERATOR = "-";

    /**
     * Generate an ID given an entity type prefix and a vertex.
     * 
     * @param entityTypePrefix
     * @param scope
     * @param data
     * @return
     * @throws IdGenerationError
     *             TODO
     */
    public String generateId(String entityTypePrefix, PermissionScope scope,
            Map<String, Object> data) throws IdGenerationError;
}
