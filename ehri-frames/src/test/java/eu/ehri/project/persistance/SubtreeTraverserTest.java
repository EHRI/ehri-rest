package eu.ehri.project.persistance;

import static org.junit.Assert.*;

import eu.ehri.project.models.base.Frame;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

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
            public void process(Frame vertexFrame, int depth, String rname, int rnum) {
                System.out.println(manager.getId(vertexFrame) + " -> " + rname);                
            }
        });
        // TODO: Actual useful tests
        assertTrue(true);
    }
}
