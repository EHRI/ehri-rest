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

package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.test.GraphTestBase;
import eu.ehri.project.utils.GraphInitializer;
import org.apache.commons.cli.CommandLine;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class LoadFixturesTest extends GraphTestBase {

    private FramedGraph<? extends TransactionalGraph> testGraph;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        testGraph = getFramedGraph();
        new GraphInitializer(testGraph).initialize();
    }

    @Test
    public void testWithResourceFilePath() throws Exception {
        List<VertexProxy> before = getGraphState(testGraph);
        String path = getFixtureFilePath("test-fixture.yaml");
        String[] args = new String[]{path};
        LoadFixtures load = new LoadFixtures();
        CommandLine cmdLine = load.getCmdLine(args);
        assertEquals(0, load.execWithOptions(testGraph, cmdLine));

        // Check some nodes have been added...
        List<VertexProxy> after = getGraphState(testGraph);
        GraphDiff graphDiff = diffGraph(before, after);
        assertTrue(graphDiff.added.size() > 0);
        assertFalse(graphDiff.removed.size() > 0);
    }
}
