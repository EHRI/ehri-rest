/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers;

import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.base.PermissionScopeFinder;
import eu.ehri.project.importers.exceptions.ImportHierarchyMapError;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.PermissionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Permission scope finder that
 */
public class DynamicPermissionScopeFinder implements PermissionScopeFinder {

    private static final Logger logger = LoggerFactory.getLogger(DynamicPermissionScopeFinder.class);

    private final PermissionScope topLevelScope;
    private final Map<String, String> hierarchyMap;
    private final Map<String, PermissionScope> permissionScopeCache = Maps.newHashMap();

    public DynamicPermissionScopeFinder(PermissionScope topLevelScope, Map<String, String> hierarchyMap) {
        this.topLevelScope = topLevelScope;
        this.hierarchyMap = hierarchyMap;
    }

    @Override
    public PermissionScope apply(String localId) {
        if (!hierarchyMap.containsKey(localId)) {
            throw new ImportHierarchyMapError(String.format("Hierarchy map does not contain unit local identifier: '%s'", localId));
        }

        String parentLocalId = hierarchyMap.get(localId);
        if (parentLocalId != null) {
            PermissionScope dynamicScope = permissionScopeCache.computeIfAbsent(parentLocalId, local -> {
                final List<Accessible> collect = StreamSupport.stream(topLevelScope.getAllContainedItems().spliterator(), false)
                        .filter(s -> s.getProperty(Ontology.IDENTIFIER_KEY).equals(local))
                        .collect(Collectors.toList());
                if (collect.size() > 1) {
                    throw new ImportHierarchyMapError("Hierarchy local identifiers are not unique.");
                } else if (collect.isEmpty()) {
                    throw new ImportHierarchyMapError(String.format("Hierarchy local identifier '%s' not found in scope: %s", local, topLevelScope.getId()));
                } else {
                    return collect.get(0).as(PermissionScope.class);
                }
            });
            logger.debug("Found dynamic scope for {}: {}", localId, dynamicScope.getId());
            return dynamicScope;
        }
        return topLevelScope;
    }
}
