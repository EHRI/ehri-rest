package eu.ehri.project.utils.fixtures.impl;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.ehri.project.models.base.Frame;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.utils.GraphInitializer;

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
 * @author michaelb
 */
public class YamlFixtureLoader implements FixtureLoader {

    public static final String DEFAULT_FIXTURE_FILE = "testdata.yaml";

    private final FramedGraph<? extends TransactionalGraph> graph;
    private final GraphManager manager;
    private static final Logger logger = LoggerFactory
            .getLogger(YamlFixtureLoader.class);
    private boolean initialize;

    /**
     * Constructor
     *
     * @param graph
     * @param initialize
     */
    public YamlFixtureLoader(FramedGraph<? extends TransactionalGraph> graph, boolean initialize) {
        this.graph = graph;
        this.initialize = initialize;
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Constructor.
     *
     * @param graph
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
        if (file.exists() && file.isFile()) {
            try {
                loadTestData(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            InputStream stream = this.getClass().getClassLoader()
                    .getResourceAsStream(resourceNameOrPath);
            if (stream == null) {
                throw new IllegalArgumentException("File or resource " + resourceNameOrPath + " does not exist.");
            }
            loadTestData(stream);
        }
    }

    public void loadTestData(InputStream stream) {
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
            graph.getBaseGraph().stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
        } catch (RuntimeException e) {
            graph.getBaseGraph().stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
            e.printStackTrace();
            throw e;
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
                        logger.debug("Importing node: {}", node);
                        importNode(links, (Map<String, Object>) node);
                    }
                }
            }

            // Finally, go through and wire up all the non-dependent
            // relationships
            logger.debug("Linking data...");
            for (Entry<Vertex, ListMultimap<String, String>> entry : links.entrySet()) {
                logger.debug("Setting links for: {}", entry.getKey());
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
            logger.debug(String.format(" - %s -[%s]-> %s", src, dst,
                    relname));
            graph.addEdge(null, src, dst, relname);
            graph.getBaseGraph().stopTransaction(
                    TransactionalGraph.Conclusion.SUCCESS);
        }
    }

    /**
     * @param links
     * @param node
     * @throws DeserializationError
     * @throws ValidationError
     * @throws IntegrityError
     * @throws ItemNotFound
     */
    private void importNode(Map<Vertex, ListMultimap<String, String>> links,
            Map<String, Object> node) throws DeserializationError,
            ValidationError, IntegrityError, ItemNotFound {
        String id = (String) node.get(Bundle.ID_KEY);
        EntityClass isa = EntityClass.withName((String) node
                .get(Bundle.TYPE_KEY));
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
        BundleDAO persister = new BundleDAO(graph, SystemScope.getInstance());
        logger.debug("Creating node with id: {}", id);
        Frame frame = persister.createOrUpdate(entityBundle,
                Frame.class);

        ListMultimap<String, String> linkRels = getLinkedRelations(nodeRels);
        if (!linkRels.isEmpty()) {
            links.put(frame.asVertex(), linkRels);
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
        return Bundle.fromData(data);
    }

    /**
     * Extract from the relations the IDs of other non-dependent nodes.
     *
     * @param data
     * @return
     */
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

    /**
     * Extract from the relations the nested dependent items.
     *
     * @param data
     * @return
     */
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

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    public void loadTestData() {
            loadFixtures();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("No Neo4j database specified.");
        }

        GraphDatabaseService db = new GraphDatabaseFactory()
                .newEmbeddedDatabase(args[0]);
        registerShutdownHook(db);
        try {
            FramedGraph<Neo4jGraph> graph = new FramedGraph<Neo4jGraph>(
                    new Neo4jGraph(db));
            YamlFixtureLoader loader = new YamlFixtureLoader(graph);
            loader.loadTestData();
        } catch (Error e) {
            db.shutdown();
            throw new RuntimeException(e);
        }
        System.exit(0);
    }
}
