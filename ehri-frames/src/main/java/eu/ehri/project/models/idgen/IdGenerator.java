package eu.ehri.project.models.idgen;

import java.util.Map;

import eu.ehri.project.exceptions.IdGenerationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.Bundle;

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
    public static final String SEPARATOR = "-";


    /**
     * Handle an id collision by either a validation error depending
     * on how the id was generated, or a RuntimeError.
     * @param type
     * @param scope
     * @param bundle
     */
    public void handleIdCollision(EntityClass type, PermissionScope scope,
            Bundle bundle) throws ValidationError;

    /**
     * Generate an ID given an entity type prefix and a vertex.
     * 
     * @param type
     * @param scope
     * @param bundle
     * @return
     */
    public String generateId(EntityClass type, PermissionScope scope,
            Bundle bundle);
}
