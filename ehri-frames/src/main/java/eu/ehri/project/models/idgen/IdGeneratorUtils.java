package eu.ehri.project.models.idgen;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Messages;
import eu.ehri.project.utils.Slugify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * Delegation functions for ID generation.
 * 
 * @author michaelb
 * 
 */
public class IdGeneratorUtils  {
    /**
     * Separate or ID components.
     */
    public static final String SEPARATOR = "-";

    protected static Logger logger = LoggerFactory.getLogger(IdGeneratorUtils.class);

    public static ListMultimap<String,String> handleIdCollision(PermissionScope scope,
            String dataKey, String ident) {

        String scopeId = scope == null ? "none" : scope.getIdentifier();
        logger.error("ID Generation error: {}={} (scope: {})", dataKey, ident, scopeId);
        ListMultimap<String,String> errors = LinkedListMultimap.create();
        errors.put(dataKey,  MessageFormat.format(
                Messages.getString("BundleDAO.uniquenessError"), ident));
        return errors;
    }


    /**
     * Uses the items identifier and its entity type to generate a (supposedly)
     * unique ID.
     */
    public static String generateId(PermissionScope scope, Bundle bundle, String ident) {
        LinkedList<String> scopeIds = Lists.newLinkedList();
        if (scope != null && !scope.equals(SystemScope.getInstance())) {
            for (PermissionScope s : scope.getPermissionScopes())
                scopeIds.addFirst(s.getIdentifier());
            scopeIds.add(scope.getIdentifier());            
        }
        return generateId(scopeIds, bundle, ident);
    }

    /**
     * Use an array of scope IDs and the bundle data to generate a unique
     * id within a given scope.
     *
     * @param scopeIds An array of scope ids
     * @param bundle The input bundle
     * @return The complete id string
     */
    public static String generateId(final List<String> scopeIds, final Bundle bundle, String ident) {

        // Validation should have ensured that ident exists...
        if (ident == null || ident.trim().isEmpty()) {
            throw new RuntimeException("Invalid null identifier for "
                    + bundle.getType().getName() + ": " + bundle.getData());
        }
        List<String> newIds = Lists.newArrayList(scopeIds);
        newIds.add(ident);
        String scopedId =  Joiner.on(SEPARATOR).join(newIds);
        return Slugify.slugify(scopedId);
    }
}
