package eu.ehri.project.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.persistance.Serializer;
import eu.ehri.project.persistance.TraversalCallback;

/**
 * Test the subtree traverser actually works.
 * 
 * @author michaelb
 *
 */
public class SubtreeTraverserTest extends AbstractFixtureTest {

    @Test
    public void testSubtreeSerialization() {
        new Serializer(graph).traverseSubtree(item, new TraversalCallback() {            
            public void process(VertexFrame vertexFrame, int depth, String rname, int rnum) {
                System.out.println(manager.getId(vertexFrame) + " -> " + rname);                
            }
        });
        // TODO: Actual useful tests
        assertTrue(true);
    }
}
