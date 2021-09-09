/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.models.idgen;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
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
import java.util.Collection;
import java.util.List;

/**
 * Delegation functions for ID generation.
 */
public class IdGeneratorUtils {
    /**
     * Separator for ID components.
     */
    public static final String HIERARCHY_SEPARATOR = "-";
    public static final String SLUG_REPLACE = "_";

    private static final Joiner hierarchyJoiner = Joiner.on(HIERARCHY_SEPARATOR);

    protected final static Logger logger = LoggerFactory.getLogger(IdGeneratorUtils.class);

    public static ListMultimap<String, String> handleIdCollision(Collection<String> scopeIds,
            List<String> dataKeys, String ident) {
        logger.error("ID Generation error: {}={} (scope: {})", dataKeys, ident, Lists.newArrayList(scopeIds));
        ListMultimap<String, String> errors = ArrayListMultimap.create();
        for (String key : dataKeys) {
            errors.put(key, MessageFormat.format(
                    Messages.getString("BundleManager.uniquenessError"), ident));
        }
        return errors;
    }


    /**
     * Uses the items identifier and its entity type to generate a (supposedly)
     * unique ID.
     *
     * @param scope  the permission scope
     * @param bundle the item's data bundle
     * @param ident  the item's identifier
     * @return a unique ID incorporating the item's local identifier and its scope
     */
    public static String generateId(PermissionScope scope, Bundle bundle, String ident) {
        List<String> scopeIds = Lists.newArrayList();
        if (scope != null && !scope.equals(SystemScope.getInstance())) {
            for (PermissionScope s : scope.getPermissionScopes()) {
                scopeIds.add(0, s.getIdentifier());
            }
            scopeIds.add(scope.getIdentifier());
        }
        return generateId(scopeIds, bundle, ident);
    }

    /**
     * Use an array of scope IDs and the bundle data to generate a unique
     * id within a given scope.
     *
     * @param scopeIds An array of scope ids
     * @param bundle   The input bundle
     * @param ident    the item's identifier
     * @return The complete id string
     */
    public static String generateId(Collection<String> scopeIds, Bundle bundle, String ident) {

        // Validation should have ensured that ident exists...
        if (ident == null || ident.trim().isEmpty()) {
            throw new RuntimeException("Invalid null identifier for "
                    + bundle.getType().getName() + ": " + bundle.getData());
        }
        List<String> newIds = Lists.newArrayList(scopeIds);
        newIds.add(ident);
        return joinPath(newIds);
    }

    /**
     * Join an identifier path to form a full ID.
     * Duplicate parts of the part are removed.
     * This check is case sensitive.
     *
     * @param path A non-empty list of identifier strings.
     * @return The resultant path ID.
     */
    public static String joinPath(Collection<String> path) {
        // If the last part of the path is included in the
        // current part, remove that chunk, providing that
        // there is something left over.
        List<String> newPaths = Lists.newArrayList();
        String last = null;
        for (String ident : path) {
            if (last == null) {
                newPaths.add(ident);
            } else {
                if (ident.startsWith(last) && !ident.equals(last)) {
                    newPaths.add(ident.substring(last.length()));
                } else {
                    newPaths.add(ident);
                }
            }
            last = ident;
        }

        // Slugify the path sections...
        List<String> slugged = Lists.newArrayList();
        for (String p : newPaths) {
            slugged.add(Slugify.slugify(p, SLUG_REPLACE));
        }

        return hierarchyJoiner.join(slugged);
    }
}
