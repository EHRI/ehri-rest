package eu.ehri.project.models.idgen;

import com.google.common.collect.ListMultimap;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.Bundle;

import java.util.List;

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
    public ListMultimap<String,String> handleIdCollision(EntityClass type, PermissionScope scope,
            Bundle bundle);

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

    /**
     * Generate an ID given an array of scope IDs. This can be used
     * where the scope might not yet exist.
     *
     * @param type
     * @param scopeIds array of scope ids, ordered parent-to-child.
     * @param bundle
     * @return
     */
    public String generateId(EntityClass type, List<String> scopeIds,
            Bundle bundle);

}
