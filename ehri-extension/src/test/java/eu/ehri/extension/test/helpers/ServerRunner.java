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

package eu.ehri.extension.test.helpers;

import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.core.Tx;
import eu.ehri.project.core.TxGraph;
import eu.ehri.project.core.impl.TxNeo4jGraph;
import eu.ehri.project.test.utils.GraphCleaner;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.eclipse.jetty.util.log.Log;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.helpers.CommunityServerBuilder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to obtain a server.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ServerRunner {

    public final static org.slf4j.Logger logger = LoggerFactory.getLogger(ServerRunner.class);
    private static ServerRunner INSTANCE = null;

    // Graph factory.
    final static FramedGraphFactory graphFactory = new FramedGraphFactory(new JavaHandlerModule());

    private final int port;
    private final String jaxRxPackage;
    private final String mountPoint;

    private Level logLevel = Level.OFF;

    private FramedGraph<? extends TxGraph> framedGraph;
    private static FixtureLoader fixtureLoader = null;
    private static GraphCleaner<? extends IndexableGraph> graphCleaner = null;

    private final static Logger sunLogger = Logger.getLogger("com.sun.jersey");
    private final static Logger neoLogger = Logger.getLogger("org.neo4j.server");
    private final static ch.qos.logback.classic.Logger graphLogger =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(TxNeo4jGraph.class);

    private CommunityNeoServer neoServer;

    private ServerRunner(int port, String jaxRxPackage, String mountPoint) {
        this.port = port;
        this.jaxRxPackage = jaxRxPackage;
        this.mountPoint = mountPoint;
    }

    public static ServerRunner getInstance(int port, String jaxRxPackage, String mountPoint) {
        if (INSTANCE == null) {
            INSTANCE = new ServerRunner(port, jaxRxPackage, mountPoint);
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
        graphLogger.setLevel(ch.qos.logback.classic.Level.ERROR);

        // This combination of stuff seems to quiet Jetty's log.
        Log.__logClass = "none";
        Properties p = new Properties();
        p.setProperty("org.eclipse.jetty.LEVEL", "WARN");
        org.eclipse.jetty.util.log.StdErrLog.setProperties(p);

        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        neoServer = CommunityServerBuilder.server()
                .onPort(port)
                .withThirdPartyJaxRsPackage(jaxRxPackage, "/" + mountPoint)
                .build();
        neoServer.start();

        TxGraph graph = new TxNeo4jGraph(neoServer.getDatabase().getGraph());
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
