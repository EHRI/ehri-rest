package eu.ehri.project.importers;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Maps;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.importers.base.PermissionScopeFinder;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.PermissionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Permission scope finder that
 */
public class DynamicPermissionScopeFinder implements PermissionScopeFinder {
    // Build a schema for header-less TSV
    private static final CsvSchema hierarchyReaderSchema = CsvSchema.emptySchema()
            .withoutHeader()
            .withLineSeparator("\n")
            .withColumnSeparator('\t');

    private static final Logger logger = LoggerFactory.getLogger(DynamicPermissionScopeFinder.class);

    private final PermissionScope topLevelScope;

    private Map<String, String> hierarchyCache = null;
    private final Map<String, PermissionScope> permissionScopeCache = Maps.newHashMap();

    private DynamicPermissionScopeFinder(PermissionScope topLevelScope, Map<String, String> hierarchyMap) {
        this.topLevelScope = topLevelScope;
        hierarchyCache = hierarchyMap;
    }

    public static DynamicPermissionScopeFinder fromTsv(PermissionScope topLevelScope, String tsvText) throws IOException {
        return new DynamicPermissionScopeFinder(topLevelScope, loadHierarchyCache(tsvText) );
    }

    public static DynamicPermissionScopeFinder fromUri(PermissionScope topLevelScope, URI uri) throws IOException {
        return fromTsv(topLevelScope, readUri(uri));
    }

    @Override
    public PermissionScope get(String localId) {
        String parentLocalId = hierarchyCache.get(localId);
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

    private static Map<String, String> loadHierarchyCache(String tsvText) throws IOException {
        CsvMapper mapper = new CsvMapper();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        Map<String, String> data = Maps.newHashMap();
        try (MappingIterator<String[]> iterator = mapper
                .readerFor(String[].class)
                .with(hierarchyReaderSchema)
                .readValues(tsvText) ) {

            while (iterator.hasNext()) {
                final String[] row = iterator.next();
                String id = row[0];
                if (row.length > 1 && row[1] != null) {
                    String parent = row[1].isEmpty() ? null : row[1];
                    data.put(id, parent);
                }
            }
            return data;
        }
    }

    private static String readUri(URI uri) throws IOException {
        try (InputStream s = uri.toURL().openStream()) {
            return new BufferedReader(
                    new InputStreamReader(s, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        }
    }
}
