package eu.ehri.project.models.idgen;

import java.text.MessageFormat;
import java.util.LinkedList;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.utils.Slugify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.Messages;
import static eu.ehri.project.models.base.IdentifiableEntity.IDENTIFIER_KEY;

/**
 * Generates an ID for nodes which represent AccessibleEntities.
 * 
 * @author michaelb
 * 
 */
public enum IdentifiableEntityIdGenerator implements IdGenerator {

    INSTANCE;

    private static Logger logger = LoggerFactory.getLogger(IdentifiableEntityIdGenerator.class);

    public void handleIdCollision(EntityClass type, PermissionScope scope,
            Bundle bundle) throws ValidationError {
        String scopeId = scope == null ? "none" : scope.getIdentifier();
        logger.error("ID Generation error: {}={} (scope: {})", IDENTIFIER_KEY,
                bundle.getDataValue(IDENTIFIER_KEY), scopeId);
        ListMultimap<String,String> errors = LinkedListMultimap.create();
        errors.put(IDENTIFIER_KEY,  MessageFormat.format(
                Messages.getString("BundleDAO.uniquenessError"), //$NON-NLS-1$
                bundle.getDataValue(IDENTIFIER_KEY)));
        throw new ValidationError(bundle, errors);
    }


    /**
     * Uses the items identifier and its entity type to generate a (supposedly)
     * unique ID.
     */
    public String generateId(EntityClass type, PermissionScope scope,
            Bundle bundle) {
        LinkedList<String> scopeIds = Lists.newLinkedList();
        if (scope != null && !scope.equals(SystemScope.getInstance())) {
            for (PermissionScope s : scope.getPermissionScopes())
                scopeIds.addFirst(s.getIdentifier());
            scopeIds.add(scope.getIdentifier());            
        }
        // TODO: Should be slugify IDs? This would make relating items to
        // their ID a bit harder but lead to cleaner IDs. Not doing this now
        // because having dirty IDs is an effective way of debugging (via
        // breakage) other parts of the system.
        String ident = (String) bundle.getDataValue(IDENTIFIER_KEY);
        // Validation should have ensured that ident exists...
        if (ident == null || ident.trim().isEmpty()) {
            throw new RuntimeException("Invalid null identifier for AccessibleEntity: " + bundle.getData());
        }
        scopeIds.add(ident);
        String scopedId =  Joiner.on(SEPARATOR).join(scopeIds);
        return Slugify.slugify(scopedId);
    }
}
