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

package eu.ehri.project.utils.fixtures.impl;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.test.GraphTestBase;
import eu.ehri.project.utils.GraphInitializer;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class YamlFixtureLoaderTest extends GraphTestBase {

    private FramedGraph<? extends TransactionalGraph> testGraph;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testGraph = getFramedGraph();
    }

    @Test
    public void testWithInitializationError() throws Exception {
        try {
            assertEquals(0, getNodeCount(testGraph));
            new GraphInitializer(testGraph).initialize();
            FixtureLoader loader = new YamlFixtureLoader(testGraph);
            loader.loadTestData();
            fail("Loading fixtures on an initialized graph should have" +
                    " thrown an integrity error");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Integrity error"));
        }
    }

    @Test
    public void testWithInitialization() throws Exception {
        // NB: An initialized Neo4j 1.9 graph has a root node
        assertEquals(0, getNodeCount(testGraph));
        new GraphInitializer(testGraph).initialize();
        FixtureLoader loader = new YamlFixtureLoader(testGraph)
            .setInitializing(false);
        loader.loadTestData();
        assertTrue(getNodeCount(testGraph) > 1);
    }

    @Test
    public void testLoadTestDataFile() throws Exception {
        assertEquals(0, getNodeCount(testGraph));
        FixtureLoader loader = new YamlFixtureLoader(testGraph);
        loader.loadTestData(getFixtureFilePath("testdata.yaml"));
        assertTrue(getNodeCount(testGraph) > 1);
    }

    @Test
    public void testLoadTestDataResource() throws Exception {
        assertEquals(0, getNodeCount(testGraph));
        FixtureLoader loader = new YamlFixtureLoader(testGraph);
        loader.loadTestData("testdata.yaml");
        assertTrue(getNodeCount(testGraph) > 1);
    }

    @Test
    public void testLoadTestDataStream() throws Exception {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("testdata.yaml");
        assertNotNull(inputStream);

        assertEquals(0, getNodeCount(testGraph));
        FixtureLoader loader = new YamlFixtureLoader(testGraph);
        loader.loadTestData(inputStream);
        assertTrue(getNodeCount(testGraph) > 1);
    }
}
