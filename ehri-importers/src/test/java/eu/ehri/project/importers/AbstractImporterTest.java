package eu.ehri.project.importers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.cvoc.Concept;
import eu.ehri.project.test.AbstractFixtureTest;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author linda
 */
public class AbstractImporterTest extends AbstractFixtureTest {
    private static final Logger logger = LoggerFactory.getLogger(AbstractImporterTest.class);

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

    protected static class GraphDiff {
        final Set<VertexProxy> added;
        final Set<VertexProxy> removed;

        public GraphDiff(Set<VertexProxy> added, Set<VertexProxy> removed) {
            this.added = added;
            this.removed = removed;
        }

        public void printDebug(PrintStream printStream) {
            printDebug(printStream, false);
        }

        public void printDebug(PrintStream printStream, boolean verbose) {
            if (added.isEmpty() && removed.isEmpty()) {
                printStream.println("GraphDiff - No nodes added or removed");
            } else {
                printStream.println("GraphDiff - Nodes added: " + added.size());
                for (VertexProxy proxy : added) {
                    printStream.println(verbose ? proxy.toStringVerbose() : proxy.toString());
                }
                printStream.println("GraphDiff - Nodes removed: " + removed.size());
                for (VertexProxy proxy : removed) {
                    printStream.println(verbose ? proxy.toStringVerbose() : proxy.toString());
                }
            }
        }
    }

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

    protected void printGraph(FramedGraph<?> graph) {
        int vcount = 0;
        for (Vertex v : graph.getVertices()) {
            logger.debug(++vcount + " -------------------------");
            for (String key : v.getPropertyKeys()) {
                String value = "";
                if (v.getProperty(key) instanceof String[]) {
                    String[] list = (String[]) v.getProperty(key);
                    for (String o : list) {
                        value += "[" + o + "] ";
                    }
                } else {
                    value = v.getProperty(key).toString();
                }
                logger.debug(key + ": " + value);
            }

            for (Edge e : v.getEdges(Direction.OUT)) {
                logger.debug(e.getLabel());
            }
        }
    }

    // Print a Concept Tree from a 'top' concept down into all narrower concepts
    public void printConceptTree(final PrintStream out, Concept c) {
        printConceptTree(out, c, 0, "");
    }

    public void printConceptTree(final PrintStream out, Concept c, int depth, String indent) {
        if (depth > 100) {
            out.println("STOP RECURSION, possibly cyclic 'tree'");
            return;
        }

        out.print(indent);
        out.print("[" + c.getIdentifier() + "]");
        Iterable<Description> descriptions = c.getDescriptions();
        for (Description d : descriptions) {
            String lang = d.getLanguageOfDescription();
            String prefLabel = (String) d.asVertex().getProperty(Ontology.PREFLABEL); // can't use the getPrefLabel() !
            out.print(", \"" + prefLabel + "\"(" + lang + ")");
        }
        // TODO Print related concept ids?
        for (Concept related : c.getRelatedConcepts()) {
            out.print("[" + related.getIdentifier() + "]");
        }
        for (Concept relatedBy : c.getRelatedByConcepts()) {
            out.print("[" + relatedBy.getIdentifier() + "]");
        }

        out.println("");// end of concept

        indent += ".   ";// the '.' improves readability, but the whole printing could be improved
        for (Concept nc : c.getNarrowerConcepts()) {
            printConceptTree(out, nc, ++depth, indent); // recursive call
        }
    }

    /**
     * 
     * @param graph
     * @param identifier the value of the property 'identifier'
     * @return 
     */
    protected Vertex getVertexByIdentifier(FramedGraph<?> graph, String identifier) {
        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, identifier);
        return docs.iterator().next();
    }
    /**
     * 
     * @param graph
     * @param id the value of the property '__ID__'
     * @return 
     */
    protected Vertex getVertexById(FramedGraph<?> graph, String id) {
        Iterable<Vertex> docs = graph.getVertices(EntityType.ID_KEY, id);
        return docs.iterator().next();
    }
    protected int getNodeCount(FramedGraph<?> graph) {
        long l = Iterables.count(graph.getVertices());
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE)
            throw new RuntimeException("Too many vertex items in graph to fit into an integer!");
        return (int)l;
    }

    protected int getEdgeCount(FramedGraph<?> graph) {
        long l = Iterables.count(graph.getEdges());
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE)
            throw new RuntimeException("Too many edge items in graph to fit into an integer!");
        return (int)l;
    }
}
