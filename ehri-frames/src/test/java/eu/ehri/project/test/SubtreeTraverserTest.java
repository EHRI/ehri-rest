package eu.ehri.project.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.persistance.Serializer;
import eu.ehri.project.persistance.TraversalCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the subtree traverser actually works.
 * 
 * @author michaelb
 *
 */
public class SubtreeTraverserTest extends AbstractFixtureTest {

    private static final Logger logger = LoggerFactory.getLogger(SubtreeTraverserTest.class);
    @Test
    public void testSubtreeSerialization() {
        new Serializer(graph).traverseSubtree(item, new TraversalCallback() {            
            public void process(VertexFrame vertexFrame, int depth, String rname, int rnum) {
                logger.debug(manager.getId(vertexFrame) + " -> " + rname);                
            }
        });
        // TODO: Actual useful tests
        assertTrue(true);
    }
}
