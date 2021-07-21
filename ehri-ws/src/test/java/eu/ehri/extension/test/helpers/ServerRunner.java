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

package eu.ehri.extension.test.helpers;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.core.Tx;
import eu.ehri.project.core.TxGraph;
import eu.ehri.project.core.impl.TxNeo4jGraph;
import eu.ehri.project.test.utils.GraphCleaner;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.apache.commons.io.FileUtils;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilder;
import org.neo4j.harness.Neo4jBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to obtain a server.
 */
public class ServerRunner {

    private static ServerRunner INSTANCE;

    // Graph factory.
    private final static FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule());

    private final Map<String, String> packageMountPoints;

    private Level logLevel = Level.OFF;

    private FramedGraph<? extends TxGraph> framedGraph;
    private static FixtureLoader fixtureLoader;
    private static GraphCleaner<? extends Graph> graphCleaner;

    private final static Logger sunLogger = Logger.getLogger("com.sun.jersey");
    private final static Logger neoLogger = Logger.getLogger("org.neo4j.server");
    private final static Logger graphLogger = Logger.getLogger(TxNeo4jGraph.class.getName());

    private Neo4j neo4j;
    private Path path;

    private ServerRunner(Map<String, String> packageMountPoints) {
        this.packageMountPoints = packageMountPoints;
    }

    /**
     * Get an instance of the server runner.
     *
     * @param packageMountPoints a set of package-name to mount-point mappings
     * @return a new server runner
     */
    public static ServerRunner getInstance(Map<String, String> packageMountPoints) {
        if (INSTANCE == null) {
            INSTANCE = new ServerRunner(packageMountPoints);
        }
        return INSTANCE;
    }

    public void start() throws IOException {
        if (neo4j != null) {
            throw new IOException("Server is already running: " + neo4j.httpURI());
        }
        sunLogger.setLevel(logLevel);
        neoLogger.setLevel(logLevel);

        // The TxNeo4jGraph gives a WARNING about restarted transactions when
        // doing indexing operations (e.g. when loading fixtures or resetting
        // the graph data.) This is noisy so we lower the log level here.
        graphLogger.setLevel(logLevel);

        path = Files.createTempDirectory("neo4j-tmp");
        Neo4jBuilder serverBuilder = Neo4jBuilders.newInProcessBuilder(path);

        for (Map.Entry<String, String> entry : packageMountPoints.entrySet()) {
            String mountPoint = entry.getValue().startsWith("/")
                     ? entry.getValue() : "/" + entry.getValue();
            serverBuilder = serverBuilder
                    .withUnmanagedExtension(mountPoint, entry.getKey());
        }

        neo4j = serverBuilder.build();

        DatabaseManagementService dbms = neo4j.databaseManagementService();
        GraphDatabaseService service = neo4j.defaultDatabaseService();
        TxGraph graph = new TxNeo4jGraph(dbms, service);
        framedGraph = graphFactory.create(graph);
        fixtureLoader = FixtureLoaderFactory.getInstance(framedGraph);
        graphCleaner = new GraphCleaner<>(framedGraph);
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    public String baseUri() {
        return neo4j != null ? neo4j.httpURI().toString() : null;
    }

    public void setUpData() {
        if (fixtureLoader != null) {
            try (Tx tx = framedGraph.getBaseGraph().beginTx()) {
                fixtureLoader.loadTestData();
                tx.success();
            }
        }
    }

    public void tearDownData() {
        if (graphCleaner != null) {
            try (Tx tx = framedGraph.getBaseGraph().beginTx()) {
                graphCleaner.clean();
                tx.success();
            }
        }
    }

    public void stop() {
        if (neo4j != null) {
            neo4j.close();
            neo4j = null;
            try {
                FileUtils.deleteDirectory(path.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
