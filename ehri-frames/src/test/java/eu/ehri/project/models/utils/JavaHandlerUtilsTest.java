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

package eu.ehri.project.models.utils;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class JavaHandlerUtilsTest {

    private Graph graph;

    @Before
    public void setUp() throws Exception {
        graph = TinkerGraphFactory.createTinkerGraph();
    }

    @After
    public void tearDown() throws Exception {
        graph.shutdown();
    }

    @Test
    public void testAddSingleRelationship() throws Exception {
        Vertex v1 = graph.addVertex(null);
        Vertex v2 = graph.addVertex(null);
        Vertex v3 = graph.addVertex(null);
        assertTrue(JavaHandlerUtils.addSingleRelationship(v1, v2, "test"));
        assertFalse(JavaHandlerUtils.addSingleRelationship(v1, v2, "test"));
        assertFalse(JavaHandlerUtils.addSingleRelationship(v1, v1, "test"));

        assertTrue(JavaHandlerUtils.addSingleRelationship(v1, v3, "test"));
        assertTrue(Iterables.contains(v1.getVertices(Direction.OUT), v3));
        assertFalse(Iterables.contains(v1.getVertices(Direction.OUT), v2));

        assertTrue(JavaHandlerUtils.addSingleRelationship(v2, v3, "test"));
        assertTrue(Iterables.contains(v3.getVertices(Direction.IN), v1));
        assertTrue(Iterables.contains(v3.getVertices(Direction.IN), v2));
    }
}
