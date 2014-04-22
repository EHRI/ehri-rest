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
    public static class Proxy {
        final String id;
        final String type;
        final Object gid;
        final Map<String,Object> data;
        public Proxy(Vertex v) {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Proxy proxy = (Proxy) o;

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

    public List<Proxy> getGraphState(FramedGraph<?> graph) {
        List<Proxy> list = Lists.newArrayList();
        for (Vertex v : graph.getVertices()) {
            list.add(new Proxy(v));
        }
        return list;
    }

    public void diffGraph(List<Proxy> list1, List<Proxy> list2) {
        Set<Proxy> added = Sets.newHashSet(list2);
        added.removeAll(list1);
        for (Proxy proxy : added) {
            logger.debug(" - Added:   " + proxy);
        }
        Set<Proxy> removed = Sets.newHashSet(list1);
        removed.removeAll(list2);
        for (Proxy proxy : removed) {
            logger.debug(" - Removed: " + proxy);
        }
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

    protected Vertex getVertexByIdentifier(FramedGraph<?> graph, String id) {
        Iterable<Vertex> docs = graph.getVertices(Ontology.IDENTIFIER_KEY, id);
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
