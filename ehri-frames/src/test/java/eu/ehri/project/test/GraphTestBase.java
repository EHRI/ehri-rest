package eu.ehri.project.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.annotations.EntityType;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.io.PrintStream;
import java.util.*;

/**
 * User: michaelb
 */
public abstract class GraphTestBase {

    private static final FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule());

    protected FramedGraph<? extends TransactionalGraph> graph;
    protected GraphManager manager;

    protected static List<VertexProxy> getGraphState(FramedGraph<?> graph) {
        List<VertexProxy> list = Lists.newArrayList();
        for (Vertex v : graph.getVertices()) {
            list.add(new VertexProxy(v));
        }
        return list;
    }

    protected static GraphDiff diffGraph(Collection<VertexProxy> list1, Collection<VertexProxy> list2) {
        Set<VertexProxy> added = Sets.newHashSet(list2);
        added.removeAll(list1);
        Set<VertexProxy> removed = Sets.newHashSet(list1);
        removed.removeAll(list2);
        return new GraphDiff(added, removed);
    }

    @Before
    public void setUp() throws Exception {
        graph = getFramedGraph();
        manager = GraphManagerFactory.getInstance(graph);
    }

    protected FramedGraph<? extends TransactionalGraph> getFramedGraph() {
        GraphDatabaseService rawGraph = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                .newGraphDatabase();
        return graphFactory.create(new Neo4jGraph(rawGraph));
    }

    @After
    public void tearDown() throws Exception {
        graph.shutdown();
    }

    /**
     * Util class to hold node data (for debugging the graph)
     */
    protected static class VertexProxy {
        final String id;
        final String type;
        final Object gid;
        final Map<String,Object> data;
        public VertexProxy(Vertex v) {
            this.id = v.getProperty(EntityType.ID_KEY);
            this.type = v.getProperty(EntityType.TYPE_KEY);
            this.gid = v.getId();
            data = Maps.newHashMap();
            for (String k : v.getPropertyKeys()) {
                if (!(k.equals(EntityType.ID_KEY) || k.equals(EntityType.TYPE_KEY))) {
                    data.put(k, v.getProperty(k));
                }
            }
        }
        public String toString() {
            return "<" + id + " (" + type + ") [" + gid + "]>";
        }

        public String toStringVerbose() {
            StringBuilder builder = new StringBuilder(toString());
            builder.append("\n");
            for (Map.Entry<String,Object> entry : data.entrySet()) {
                builder.append(String.format("  %-15s : %s\n", entry.getKey(), entry.getValue()));
            }
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VertexProxy proxy = (VertexProxy) o;

            return gid.equals(proxy.gid)
                    && !(id != null ? !id.equals(proxy.id) : proxy.id != null)
                    && !(type != null ? !type.equals(proxy.type) : proxy.type != null);
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + gid.hashCode();
            return result;
        }
    }

    public static class GraphDiff {
        public final Set<VertexProxy> added;
        public final Set<VertexProxy> removed;

        public GraphDiff(Set<VertexProxy> added, Set<VertexProxy> removed) {
            this.added = added;
            this.removed = removed;
        }

        public void printDebug(PrintStream printStream) {
            printDebug(printStream, false);
        }
        private Map<String, Integer> countDistinctTypes(Set<VertexProxy> vs){
            Map<String, Integer> counts = new HashMap<String, Integer>();
            for(VertexProxy v : vs){
                if(counts.containsKey(v.type))
                    counts.put(v.type, counts.get(v.type) + 1);
                else
                    counts.put(v.type, 1);
            }
            return counts;
        }

        public void printDebug(PrintStream printStream, boolean verbose) {
            if (added.isEmpty() && removed.isEmpty()) {
                printStream.println("GraphDiff - No nodes added or removed");
            } else {
                printStream.println("GraphDiff - Nodes added: " + added.size());
                for (VertexProxy proxy : added) {
                    printStream.println(verbose ? proxy.toStringVerbose() : proxy.toString());
                }
                Map<String, Integer> counts = countDistinctTypes(added);
                for(String key: counts.keySet()){
                    printStream.println(key + ": " + counts.get(key));
                }
                printStream.println("GraphDiff - Nodes removed: " + removed.size());
                for (VertexProxy proxy : removed) {
                    printStream.println(verbose ? proxy.toStringVerbose() : proxy.toString());
                }
                counts = countDistinctTypes(removed);
                for(String key: counts.keySet()){
                    printStream.println(key + ": " + counts.get(key));
                }
            }
        }
    }
}
