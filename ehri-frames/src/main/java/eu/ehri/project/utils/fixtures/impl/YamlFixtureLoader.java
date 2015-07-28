/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.utils.fixtures.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.utils.GraphInitializer;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Load data from YAML fixtures.
 * <p>
 * The YAML fixture format is almost identical to the plain bundle format, but
 * has some extensions to a) allow for creating non-dependent relationships, and
 * b) allow for single relations to be more naturally expressed. For example,
 * while, in the bundle format the relations for a given relation type is always
 * a list (even if there is typically only one), the YAML format allows using a
 * single item and it will be loaded as if it were a list containing just one
 * item, i.e, instead of writing
 * <p>
 * <pre>
 *     <code>
 * relationships: heldBy: - some-repo
 *     </code>
 * </pre>
 * <p>
 * we can just write:
 * <p>
 * <pre>
 *     <code>
 * relationships: heldBy: some-repo
 *     </code>
 * </pre>
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class YamlFixtureLoader implements FixtureLoader {

    private static final boolean DEFAULT_INIT = true;
    private static final String GENERATE_ID_PLACEHOLDER = "?";
    private static final String DEFAULT_FIXTURE_FILE = "testdata.yaml";

    private static final Logger logger = LoggerFactory.getLogger(YamlFixtureLoader.class);

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final BundleDAO dao;
    private final boolean initialize;

    /**
     * Constructor
     *
     * @param graph      The graph
     * @param initialize Whether or not to initialize the graph
     */
    public YamlFixtureLoader(FramedGraph<?> graph, boolean initialize) {
        this.graph = graph;
        this.initialize = initialize;
        manager = GraphManagerFactory.getInstance(graph);
        dao = new BundleDAO(graph);
    }

    /**
     * Constructor.
     *
     * @param graph The graph
     */
    public YamlFixtureLoader(FramedGraph<?> graph) {
        this(graph, DEFAULT_INIT);
    }

    /**
     * Perform graph initialization (creating the event log structure and
     * default nodes) prior to importing fixtures.
     *
     * @param initialize Whether or not to initialize the graph: default
     *                   {@value YamlFixtureLoader#DEFAULT_INIT}
     */
    public YamlFixtureLoader setInitializing(boolean initialize) {
        return new YamlFixtureLoader(graph, initialize);
    }

    /**
     * Load default fixtures.
     */
    private void loadFixtures() {
        InputStream ios = this.getClass().getClassLoader()
                .getResourceAsStream(DEFAULT_FIXTURE_FILE);
        loadTestData(ios);
    }

    /**
     * Load fixtures from a resource or file path.
     *
     * @param resourceNameOrPath Either a classloader-accessible
     *                           resource, or a local file path.
     */
    public void loadTestData(String resourceNameOrPath) {
        File file = new File(resourceNameOrPath);
        try {
            InputStream stream = file.exists() && file.isFile()
                    ? new FileInputStream(file)
                    : this.getClass().getClassLoader()
                    .getResourceAsStream(resourceNameOrPath);
            try {
                loadTestData(stream);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load test data from an input stream.
     *
     * @param stream A input stream of valid YAML data.
     */
    public void loadTestData(InputStream stream) {
        // Initialize the DB
        try {
            if (initialize) {
                new GraphInitializer(graph).initialize();
            }
            loadFixtureFileStream(stream);
            stream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFixtureFileStream(InputStream yamlStream) {
        Yaml yaml = new Yaml();
        try {
            Map<Vertex, Multimap<String, String>> links = Maps.newHashMap();
            for (Object data : yaml.loadAll(yamlStream)) {
                for (Object node : (List<?>) data) {
                    if (node instanceof Map) {
                        logger.trace("Importing node: {}", node);
                        importNode(links, (Map<String, Object>) node);
                    }
                }
            }

            // Finally, go through and wire up all the non-dependent
            // relationships
            logger.trace("Linking data...");
            for (Entry<Vertex, Multimap<String, String>> entry : links.entrySet()) {
                logger.trace("Setting links for: {}", entry.getKey());
                Vertex src = entry.getKey();
                Multimap<String, String> rels = entry.getValue();
                for (String relname : rels.keySet()) {
                    for (String target : rels.get(relname)) {
                        Vertex dst = manager.getVertex(target);
                        addRelationship(src, dst, relname);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading YAML fixture", e);
        }
    }

    private void addRelationship(Vertex src, Vertex dst, String relname) {
        boolean found = false;
        for (Vertex v : src.getVertices(Direction.OUT, relname)) {
            if (v.equals(dst)) {
                found = true;
                break;
            }
        }
        if (!found) {
            logger.trace(String.format(" - %s -[%s]-> %s", src, dst, relname));
            graph.addEdge(null, src, dst, relname);
        }
    }

    private void importNode(Map<Vertex, Multimap<String, String>> links,
            Map<String, Object> node) throws DeserializationError,
            ValidationError, IntegrityError, ItemNotFound {
        EntityClass isa = EntityClass.withName((String) node
                .get(Bundle.TYPE_KEY));

        String id = (String) node.get(Bundle.ID_KEY);

        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node
                .get(Bundle.DATA_KEY);
        if (nodeData == null) {
            nodeData = Maps.newHashMap();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeRels = (Map<String, Object>) node
                .get(Bundle.REL_KEY);

        // Since our data is written as a subgraph, we can use the
        // bundle converter to load it.
        Bundle entityBundle = createBundle(id, isa, nodeData,
                getDependentRelations(nodeRels));
        logger.trace("Creating node with id: {}", id);
        Mutation<Frame> frame = dao.createOrUpdate(entityBundle, Frame.class);

        Multimap<String, String> linkRels = getLinkedRelations(nodeRels);
        if (!linkRels.isEmpty()) {
            links.put(frame.getNode().asVertex(), linkRels);
        }
    }

    private Bundle createBundle(String id, EntityClass type,
            Map<String, Object> nodeData,
            Multimap<String, Map<?, ?>> dependentRelations) throws DeserializationError {
        Map<String, Object> data = ImmutableMap.of(
            Bundle.ID_KEY, id,
            Bundle.TYPE_KEY, type.getName(),
            Bundle.DATA_KEY, nodeData,
            Bundle.REL_KEY, dependentRelations.asMap()
        );
        Bundle b = Bundle.fromData(data);

        // If the given id is a placeholder, generate it according to type rules
        if (id.trim().contentEquals(GENERATE_ID_PLACEHOLDER)) {
            String newId = type.getIdgen().generateId(Lists.<String>newArrayList(), b);
            b = b.withId(newId);
        }
        return b;
    }

    private Multimap<String, String> getLinkedRelations(Map<String, Object> data) {
        Multimap<String, String> rels = ArrayListMultimap.create();
        if (data != null) {
            for (Entry<String, Object> entry : data.entrySet()) {
                String relName = entry.getKey();
                Object relValue = entry.getValue();
                if (relValue instanceof List) {
                    for (Object relation : (List<?>) relValue) {
                        if (relation instanceof String) {
                            rels.put(relName, (String) relation);
                        }
                    }
                } else if (relValue instanceof String) {
                    rels.put(relName, (String) relValue);
                }
            }
        }
        return rels;
    }

    private Multimap<String, Map<?, ?>> getDependentRelations(Map<String, Object> data) {
        Multimap<String, Map<?, ?>> rels = ArrayListMultimap.create();
        if (data != null) {
            for (Entry<String, Object> entry : data.entrySet()) {
                String relName = entry.getKey();
                Object relValue = entry.getValue();
                if (relValue instanceof List) {
                    for (Object relation : (List<?>) relValue) {
                        if (relation instanceof Map) {
                            rels.put(relName, (Map<?, ?>) relation);
                        }
                    }
                } else if (relValue instanceof Map) {
                    rels.put(relName, (Map<?, ?>) relValue);
                }
            }
        }
        return rels;
    }

    public void loadTestData() {
        loadFixtures();
    }
}
