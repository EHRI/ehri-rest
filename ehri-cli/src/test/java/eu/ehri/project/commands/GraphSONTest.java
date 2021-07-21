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

package eu.ehri.project.commands;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.test.GraphTestBase;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class GraphSONTest extends GraphTestBase {
    protected FixtureLoader helper;

    @Test
    public void testSaveDumpAndRead() throws Exception {

        // 1. Load the (YAML) fixtures into a graph.
        // 2. Dump it as JSON.
        // 3. Load the JSON into another graph.
        // 4. Compare the before and after graphs and check
        //     nothing's changed.

        // Setup will create the database but not load the fixtures...
        FramedGraph<? extends TransactionalGraph> graph1 = getFramedGraph();
        helper = FixtureLoaderFactory.getInstance(graph1);
        helper.loadTestData();
        graph1.getBaseGraph().commit();

        List<VertexProxy> graphState1 = getGraphState(graph1);

        File temp = File.createTempFile("temp-file-name", ".tmp");
        temp.deleteOnExit();
        assertEquals(0L, temp.length());

        String filePath = temp.getAbsolutePath();
        String[] outArgs = new String[]{"--dump", filePath};

        GraphSON graphSON = new GraphSON();
        CommandLine outCmdLine = graphSON.getCmdLine(outArgs);

        assertEquals(0, graphSON.execWithOptions(graph1, outCmdLine));
        graph1.shutdown();
        resetGraph();

        assertTrue(temp.exists());
        assertTrue(temp.length() > 0L);

        FramedGraph<? extends TransactionalGraph> graph2 = getFramedGraph();

        String[] inArgs = new String[]{"--load", filePath};
        CommandLine inCmdLine = graphSON.getCmdLine(inArgs);
        assertEquals(0, graphSON.execWithOptions(graph2, inCmdLine));

        List<VertexProxy> graphState2 = getGraphState(graph2);

        GraphDiff graphDiff = diffGraph(graphState1, graphState2);
        assertEquals(0, graphDiff.added.size());
        assertEquals(0, graphDiff.removed.size());
    }
}
