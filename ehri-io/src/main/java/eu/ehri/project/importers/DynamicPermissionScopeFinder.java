package eu.ehri.project.importers;

import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.base.PermissionScopeFinder;
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
        String parentLocalId = hierarchyMap.get(localId);
        if (parentLocalId != null) {
            PermissionScope dynamicScope = permissionScopeCache.computeIfAbsent(parentLocalId, local -> {
                final List<Accessible> collect = StreamSupport.stream(topLevelScope.getAllContainedItems().spliterator(), false)
                        .filter(s -> s.getProperty(Ontology.IDENTIFIER_KEY).equals(local))
                        .collect(Collectors.toList());
                if (collect.size() > 1) {
                    throw new RuntimeException("Hierarchy local identifiers are not unique.");
                } else if (collect.isEmpty()) {
                    throw new RuntimeException(String.format("Hierarchy local identifier '%s' not found in scope: %s", local, topLevelScope.getId()));
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
