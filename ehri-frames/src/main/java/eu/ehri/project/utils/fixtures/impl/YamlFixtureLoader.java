package eu.ehri.project.utils.fixtures.impl;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.TransactionalGraph;
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

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Load data from YAML fixtures.
 * <p/>
 * FIXME: Try and clean up the rather horrible code in here.
 * <p/>
 * The YAML fixture format is almost identical to the plain bundle format, but
 * has some extensions to a) allow for creating non-dependent relationships, and
 * b) allow for single relations to be more naturally expressed. For example,
 * while, in the bundle format the relations for a given relation type is always
 * a list (even if there is typically only one), the YAML format allows using a
 * single item and it will be loaded as if it were a list containing just one
 * item, i.e, instead of writing
 * <p/>
 * relationships: heldBy: - some-repo
 * <p/>
 * we can just write:
 * <p/>
 * relationships: heldBy: some-repo
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class YamlFixtureLoader implements FixtureLoader {

    public static final String DEFAULT_FIXTURE_FILE = "testdata.yaml";

    public static final String GENERATE_ID_PLACEHOLDER = "?";

    private final FramedGraph<? extends TransactionalGraph> graph;
    private final GraphManager manager;
    private static final Logger logger = LoggerFactory
            .getLogger(YamlFixtureLoader.class);

    private boolean initialize;

    /**
     * Constructor
     *
     * @param graph      The graph
     * @param initialize Whether or not to initialize the graph
     */
    public YamlFixtureLoader(FramedGraph<? extends TransactionalGraph> graph, boolean initialize) {
        this.graph = graph;
        this.initialize = initialize;
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Constructor.
     *
     * @param graph The graph
     */
    public YamlFixtureLoader(FramedGraph<? extends TransactionalGraph> graph) {
        this(graph, true);
    }

    public void setInitializing(boolean initialize) {
        this.initialize = initialize;
    }

    private void loadFixtures() {
        InputStream ios = this.getClass().getClassLoader()
                .getResourceAsStream(DEFAULT_FIXTURE_FILE);
        loadTestData(ios);
    }

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
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadTestData(InputStream stream) {

        // Ensure we're not currently in a transaction!
        //graph.getBaseGraph().rollback();

        // Initialize the DB
        try {
            if (initialize) {
                new GraphInitializer(graph).initialize();
            }
            try {
                loadFixtureFileStream(stream);
                stream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            graph.getBaseGraph().commit();
        } catch (Exception e) {
            graph.getBaseGraph().rollback();
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFixtureFileStream(InputStream yamlStream) {
        Yaml yaml = new Yaml();
        try {
            Map<Vertex, ListMultimap<String, String>> links = Maps.newHashMap();
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
            for (Entry<Vertex, ListMultimap<String, String>> entry : links.entrySet()) {
                logger.trace("Setting links for: {}", entry.getKey());
                Vertex src = entry.getKey();
                ListMultimap<String, String> rels = entry.getValue();
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
            if (v == dst) {
                found = true;
                break;
            }
        }
        if (!found) {
            logger.trace(String.format(" - %s -[%s]-> %s", src, dst,
                    relname));
            graph.addEdge(null, src, dst, relname);
        }
    }

    private void importNode(Map<Vertex, ListMultimap<String, String>> links,
            Map<String, Object> node) throws DeserializationError,
            ValidationError, IntegrityError, ItemNotFound {
        EntityClass isa = EntityClass.withName((String) node
                .get(Bundle.TYPE_KEY));

        String id = (String) node.get(Bundle.ID_KEY);

        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) node
                .get(Bundle.DATA_KEY);
        if (nodeData == null)
            nodeData = new HashMap<String, Object>();
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeRels = (Map<String, Object>) node
                .get(Bundle.REL_KEY);

        // Since our data is written as a subgraph, we can use the
        // bundle converter to load it.
        Bundle entityBundle = createBundle(id, isa, nodeData,
                getDependentRelations(nodeRels));
        BundleDAO persister = new BundleDAO(graph);
        logger.trace("Creating node with id: {}", id);
        Mutation<Frame> frame = persister.createOrUpdate(entityBundle,
                Frame.class);

        ListMultimap<String, String> linkRels = getLinkedRelations(nodeRels);
        if (!linkRels.isEmpty()) {
            links.put(frame.getNode().asVertex(), linkRels);
        }
    }

    private Bundle createBundle(final String id, final EntityClass type,
            final Map<String, Object> nodeData,
            final ListMultimap<String, Map<?, ?>> dependentRelations) throws DeserializationError {
        @SuppressWarnings("serial")
        Map<String, Object> data = new HashMap<String, Object>() {
            {
                put(Bundle.ID_KEY, id);
                put(Bundle.TYPE_KEY, type.getName());
                put(Bundle.DATA_KEY, nodeData);
                put(Bundle.REL_KEY, dependentRelations.asMap());
            }
        };
        Bundle b = Bundle.fromData(data);

        // If the given id is a placeholder, generate it according to type rules
        if (id.trim().contentEquals(GENERATE_ID_PLACEHOLDER)) {
            String newId = type.getIdgen().generateId(Lists.<String>newArrayList(), b);
            b = b.withId(newId);
        }
        return b;
    }

    private ListMultimap<String, String> getLinkedRelations(Map<String, Object> data) {
        ListMultimap<String, String> rels = LinkedListMultimap.create();
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

    private ListMultimap<String, Map<?, ?>> getDependentRelations(Map<String, Object> data) {
        ListMultimap<String, Map<?, ?>> rels = LinkedListMultimap.create();
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
