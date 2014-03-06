package eu.ehri.project.persistence.utils;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class DataUtilsTest {
    @Test
    public void testIsEmptySequence() throws Exception {
        Object t1 = "a string";
        Object t2 = Lists.newArrayList("not empty");
        Object t3 = Lists.newArrayList();
        Object[] t4 = {"not", "empty"};
        Object[] t5 = {};
        Object t6 = Iterables.empty();
        assertFalse(DataUtils.isEmptySequence(t1));
        assertFalse(DataUtils.isEmptySequence(t2));
        assertTrue(DataUtils.isEmptySequence(t3));
        assertFalse(DataUtils.isEmptySequence(t4));
        assertTrue(DataUtils.isEmptySequence(t5));
        assertTrue(DataUtils.isEmptySequence(t6));
    }
}
