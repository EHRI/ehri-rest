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
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.helpers.CommunityServerBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

/**
 * Utility class to obtain a server.
 */
public class ServerRunner {

    private static ServerRunner INSTANCE;

    // Graph factory.
    private final static FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule());

    private final int port;
    private final Map<String, String> packageMountPoints;

    private Level logLevel = Level.OFF;

    private FramedGraph<? extends TxGraph> framedGraph;
    private static FixtureLoader fixtureLoader;
    private static GraphCleaner<? extends Graph> graphCleaner;

    private final static Logger sunLogger = Logger.getLogger("com.sun.jersey");
    private final static Logger neoLogger = Logger.getLogger("org.neo4j.server");
    private final static Logger graphLogger = Logger.getLogger(TxNeo4jGraph.class.getName());

    private CommunityNeoServer neoServer;

    private ServerRunner(int port, Map<String, String> packageMountPoints) {
        this.port = port;
        this.packageMountPoints = packageMountPoints;
    }

    /**
     * Get an instance of the server runner.
     *
     * @param port               the port
     * @param packageMountPoints a set of package-name to mount-point mappings
     * @return a new server runner
     */
    public static ServerRunner getInstance(int port, Map<String, String> packageMountPoints) {
        if (INSTANCE == null) {
            INSTANCE = new ServerRunner(port, packageMountPoints);
        }
        return INSTANCE;
    }

    public void start() throws IOException {
        if (neoServer != null) {
            throw new IOException("Server is already running: " + neoServer.baseUri());
        }
        sunLogger.setLevel(logLevel);
        neoLogger.setLevel(logLevel);

        // The TxNeo4jGraph gives a WARNING about restarted transactions when
        // doing indexing operations (e.g. when loading fixtures or resetting
        // the graph data.) This is noisy so we lower the log level here.
        graphLogger.setLevel(logLevel);

        CommunityServerBuilder serverBuilder = CommunityServerBuilder.server()
                .onAddress(new SocketAddress("localhost", port));
        for (Map.Entry<String, String> entry : packageMountPoints.entrySet()) {
            String mountPoint = entry.getValue().startsWith("/")
                    ? entry.getValue() : "/" + entry.getValue();
            serverBuilder = serverBuilder
                    .withThirdPartyJaxRsPackage(entry.getKey(), mountPoint);
        }
        neoServer = serverBuilder
                .withProperty("dbms.connector.bolt.listen_address", "0.0.0.0:7688")
                .build();
        neoServer.start();

        DatabaseManagementService dbms = neoServer.getDatabaseService().getDatabaseManagementService();
        GraphDatabaseService service = dbms.database(DEFAULT_DATABASE_NAME);
        TxGraph graph = new TxNeo4jGraph(dbms, service);
        framedGraph = graphFactory.create(graph);
        fixtureLoader = FixtureLoaderFactory.getInstance(framedGraph);
        graphCleaner = new GraphCleaner<>(framedGraph);
    }

    public CommunityNeoServer getServer() {
        return neoServer;
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
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
        if (neoServer != null) {
            neoServer.stop();
            neoServer = null;
        }
    }
}
