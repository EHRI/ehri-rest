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

package eu.ehri.project.test;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphConfiguration;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.AbstractModule;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.acl.AnonymousAccessor;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.ApiFactory;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.core.impl.Neo4jGraphManager;
import eu.ehri.project.core.impl.neo4j.Neo4j2Graph;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.utils.CustomAnnotationsModule;
import eu.ehri.project.models.utils.UniqueAdjacencyAnnotationHandler;
import org.junit.After;
import org.junit.Before;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;


public abstract class GraphTestBase {

    private static final FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule(), new CustomAnnotationsModule());

    protected FramedGraph<? extends TransactionalGraph> graph;
    protected GraphManager manager;

    protected static List<VertexProxy> getGraphState(FramedGraph<?> graph) {
        List<VertexProxy> list = Lists.newArrayList();
        for (Vertex v : graph.getVertices()) {
            // Ignore vertices that have been deleting in this TX
            // Neo4j will throw an illegal state exception here.
            try {
                list.add(new VertexProxy(v));
            } catch (IllegalStateException e) {
                if (!e.getMessage().contains("deleted in this tx")) {
                    throw e;
                }
            }
        }
        return list;
    }

    protected Api api(Accessor accessor) {
        return ApiFactory.noLogging(graph, accessor);
    }

    protected Api anonApi() {
        return api(AnonymousAccessor.getInstance());
    }

    protected Api loggingApi(Accessor accessor) {
        return api(accessor).enableLogging(true);
    }

    protected static GraphDiff diffGraph(Collection<VertexProxy> list1, Collection<VertexProxy> list2) {
        Set<VertexProxy> added = Sets.newHashSet(list2);
        added.removeAll(list1);
        Set<VertexProxy> removed = Sets.newHashSet(list1);
        removed.removeAll(list2);
        return new GraphDiff(added, removed);
    }

    protected static String getFixtureFilePath(final String resourceName) throws Exception {
        URL resource = Resources.getResource(resourceName);
        return Paths.get(resource.toURI()).toString();
    }

    @Before
    public void setUp() throws Exception {
        graph = getFramedGraph();
        manager = GraphManagerFactory.getInstance(graph);
    }

    protected FramedGraph<? extends TransactionalGraph> getFramedGraph() throws IOException {
        Path tempDir = Files.createTempDirectory("neo4j-tmp");
        tempDir.toFile().deleteOnExit();
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(tempDir.toFile()).build();
        GraphDatabaseService rawGraph = managementService.database( DEFAULT_DATABASE_NAME );
        try (Transaction tx = rawGraph.beginTx()) {
            Neo4jGraphManager.createIndicesAndConstraints(tx);
            tx.commit();
        }
        return graphFactory.create(new Neo4j2Graph(managementService, rawGraph));
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
        final Map<String, Object> data;

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
            for (Map.Entry<String, Object> entry : data.entrySet()) {
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

        private Map<String, Integer> countDistinctTypes(Set<VertexProxy> vs) {
            Map<String, Integer> counts = Maps.newHashMap();
            for (VertexProxy v : vs) {
                if (counts.containsKey(v.type))
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
                for (String key : counts.keySet()) {
                    printStream.println(key + ": " + counts.get(key));
                }
                printStream.println("GraphDiff - Nodes removed: " + removed.size());
                for (VertexProxy proxy : removed) {
                    printStream.println(verbose ? proxy.toStringVerbose() : proxy.toString());
                }
                counts = countDistinctTypes(removed);
                for (String key : counts.keySet()) {
                    printStream.println(key + ": " + counts.get(key));
                }
            }
        }
    }

    protected int getNodeCount(FramedGraph<?> graph) {
        long l = Iterables.size(graph.getVertices());
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE)
            throw new RuntimeException("Too many vertex items in graph to fit into an integer!");
        return (int) l;
    }

    protected int getEdgeCount(FramedGraph<?> graph) {
        long l = Iterables.size(graph.getEdges());
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE)
            throw new RuntimeException("Too many edge items in graph to fit into an integer!");
        return (int) l;
    }

    protected String readResourceFileAsString(String resourceName)
            throws java.io.IOException {
        URL url = Resources.getResource(resourceName);
        return Resources.toString(url, Charsets.UTF_8);
    }
}
