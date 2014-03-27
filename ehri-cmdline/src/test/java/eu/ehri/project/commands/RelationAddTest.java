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

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
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
        assertEquals(0L, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        CommandLine commandLine = relationAdd.getCmdLine(new String[]{"mike", "knows", "linda", "--allow-duplicates"});
        int retVal = relationAdd.execWithOptions(graph, commandLine);
        assertEquals(0, retVal);
        assertEquals(1L, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(1L, Iterables.size(linda.getVertices(Direction.IN, "knows")));

        relationAdd.execWithOptions(graph, commandLine);
        assertEquals(2L, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(2L, Iterables.size(linda.getVertices(Direction.IN, "knows")));
    }

    @Test
    public void testAddRelation() throws Exception {
        assertEquals(0L, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        CommandLine commandLine = relationAdd.getCmdLine(new String[]{"mike", "knows", "linda"});
        int retVal = relationAdd.execWithOptions(graph, commandLine);
        assertEquals(0, retVal);
        assertEquals(1L, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(1L, Iterables.size(linda.getVertices(Direction.IN, "knows")));

        relationAdd.execWithOptions(graph, commandLine);
        assertEquals(1L, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(1L, Iterables.size(linda.getVertices(Direction.IN, "knows")));
    }

    @Test
    public void testAddSingleRelation() throws Exception {
        assertEquals(0L, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        CommandLine commandLine1 = relationAdd.getCmdLine(new String[]{"mike", "knows", "linda"});
        int retVal = relationAdd.execWithOptions(graph, commandLine1);
        assertEquals(0, retVal);
        assertEquals(1L, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(1L, Iterables.size(linda.getVertices(Direction.IN, "knows")));

        CommandLine commandLine2 = relationAdd.getCmdLine(new String[]{"--single", "mike", "knows", "reto"});
        relationAdd.execWithOptions(graph, commandLine2);
        assertEquals(1L, Iterables.size(mike.getVertices(Direction.OUT, "knows")));
        assertEquals(0L, Iterables.size(linda.getVertices(Direction.IN, "knows")));
        assertEquals(1L, Iterables.size(reto.getVertices(Direction.IN, "knows")));
    }
}
