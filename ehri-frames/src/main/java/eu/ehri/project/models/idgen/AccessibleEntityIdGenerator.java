package eu.ehri.project.models.idgen;

import java.util.LinkedList;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

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
        LinkedList<String> scopeIds = Lists.newLinkedList();
        for (PermissionScope s : scope.getScopes())
            scopeIds.addFirst(s.getIdentifier());
        scopeIds.add(scope.getIdentifier());
        // TODO: Should be slugify IDs? This would make relating items to
        // their ID a bit harder but lead to cleaner IDs. Not doing this now
        // because having dirty IDs is an effective way of debugging (via
        // breakage) other parts of the system.
        scopeIds.add((String) data.get(AccessibleEntity.IDENTIFIER_KEY));
        return Joiner.on(SEPERATOR).skipNulls().join(scopeIds);
    }
}
