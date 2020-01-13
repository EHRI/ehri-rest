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

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.test.GraphTestBase;
import org.apache.commons.cli.CommandLine;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class RelationAddTest extends GraphTestBase {

    private Vertex mike;
    private Vertex linda;
    private Vertex reto;
    private RelationAdd relationAdd;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        relationAdd = new RelationAdd();
        mike = manager.createVertex("mike", EntityClass.USER_PROFILE, Maps.<String,Object>newHashMap());
        reto = manager.createVertex("reto", EntityClass.USER_PROFILE, Maps.<String,Object>newHashMap());
        linda = manager.createVertex("linda", EntityClass.USER_PROFILE, Maps.<String,Object>newHashMap());
    }

    @Test
    public void testAddRelationWithDuplicates() throws Exception {
        assertEquals(0, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        CommandLine commandLine = relationAdd.getCmdLine(new String[]{"mike", "knows", "linda", "--allow-duplicates"});
        int retVal = relationAdd.execWithOptions(graph, commandLine);
        assertEquals(0, retVal);
        assertEquals(1, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(1, Iterables.size(linda.getVertices(Direction.IN, "knows")));

        relationAdd.execWithOptions(graph, commandLine);
        assertEquals(2, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(2, Iterables.size(linda.getVertices(Direction.IN, "knows")));
    }

    @Test
    public void testAddRelation() throws Exception {
        assertEquals(0, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        CommandLine commandLine = relationAdd.getCmdLine(new String[]{"mike", "knows", "linda"});
        int retVal = relationAdd.execWithOptions(graph, commandLine);
        assertEquals(0, retVal);
        assertEquals(1, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(1, Iterables.size(linda.getVertices(Direction.IN, "knows")));

        relationAdd.execWithOptions(graph, commandLine);
        assertEquals(1, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(1, Iterables.size(linda.getVertices(Direction.IN, "knows")));
    }

    @Test
    public void testAddSingleRelation() throws Exception {
        assertEquals(0, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        CommandLine commandLine1 = relationAdd.getCmdLine(new String[]{"mike", "knows", "linda"});
        int retVal = relationAdd.execWithOptions(graph, commandLine1);
        assertEquals(0, retVal);
        assertEquals(1, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(1, Iterables.size(linda.getVertices(Direction.IN, "knows")));

        CommandLine commandLine2 = relationAdd.getCmdLine(new String[]{"--single", "mike", "knows", "reto"});
        relationAdd.execWithOptions(graph, commandLine2);
        assertEquals(1, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(0, Iterables.size(linda.getVertices(Direction.IN, "knows")));
        assertEquals(1, Iterables.size(reto.getVertices(Direction.IN, "knows")));
    }
}
