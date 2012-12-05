package eu.ehri.project.models.idgen;

import java.util.Map;

import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;

/**
 * Generate an ID for the DocumentaryUnit type, which is scoped by its
 * repository.
 * 
 * @author michaelb
 * 
 */
public class DocumentaryUnitIdGenerator implements IdGenerator {

    public String generateId(String entityTypePrefix, PermissionScope scope,
            Map<String, Object> data) throws IdGenerationError {
        if (scope == null)
            throw new IdGenerationError(
                    "Null or incorrect scope given for ID generation",
                    entityTypePrefix, scope, data);
        String ident = (String) data.get(AccessibleEntity.IDENTIFIER_KEY);
        if (ident == null || ident.trim().isEmpty()) {
            throw new IdGenerationError(
                    "Documentary unit 'identifier' field was missing.",
                    entityTypePrefix, scope, data);
        }
        return entityTypePrefix + SEPERATOR + scope.getIdentifier() + SEPERATOR
                + ident;
    }

}
