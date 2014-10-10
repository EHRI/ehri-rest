package eu.ehri.project.views;

import com.google.common.base.Optional;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QueryUtilsTest {

    @Test
    public void testGetTraversalPath() throws Exception {
        String notAPath = "imNotAPath";
        Optional<QueryUtils.TraversalPath> traversalPath = QueryUtils.getTraversalPath(notAPath);
        assertFalse(traversalPath.isPresent());

        String validPath = "->foo<-bar.baz";
        traversalPath = QueryUtils.getTraversalPath(validPath);
        assertTrue(traversalPath.isPresent());

        String badPath = "foo->bar.baz";
        traversalPath = QueryUtils.getTraversalPath(badPath);
        assertFalse(traversalPath.isPresent());
    }
}
