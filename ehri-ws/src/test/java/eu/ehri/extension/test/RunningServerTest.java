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

package eu.ehri.extension.test;

import com.google.common.collect.ImmutableMap;
import eu.ehri.extension.test.helpers.ServerRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;

/**
 * Test base class which starts a Neo4j server and loads the
 * resource classes as an unmanaged extension.
 */
public abstract class RunningServerTest {

    // Mount point for EHRI resources
    final static private String mountPoint = "ehri";

    private final static ServerRunner runner = ServerRunner.getInstance(
            ImmutableMap.of("eu.ehri.extension", mountPoint));

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        runner.start();
    }

    @AfterClass
    public static void shutdownDatabase() {
        runner.stop();
    }

    String getExtensionEntryPointUri() {
        return runner.baseUri() + mountPoint;
    }

    @Before
    public void setupDb() {
        runner.setUpData();
    }

    @After
    public void resetDb() {
        runner.tearDownData();
    }
}
