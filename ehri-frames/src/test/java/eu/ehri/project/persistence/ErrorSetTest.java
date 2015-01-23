package eu.ehri.project.persistence;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ErrorSetTest {
    @SuppressWarnings("unchecked")
    @Test
    public void testToData() throws Exception {
        ErrorSet es = ErrorSet.fromError("foo", "bad");
        Map<String,Object> esdata = es.toData();
        System.out.println(es.toJson());
        assertEquals("bad", ((Map<String,List<String>>)esdata.get(ErrorSet.ERROR_KEY)).get("foo").get(0));
    }

    @Test
    public void testGetErrorValue() {
        ErrorSet es = ErrorSet.fromError("foo", "bad");
        assertEquals(Lists.newArrayList("bad"), es.getErrorValue("foo"));
    }

    @Test
    public void testWithErrorValue() {
        ErrorSet es = ErrorSet.fromError("foo", "bad")
                .withErrorValue("test", "test");
        assertEquals(Lists.newArrayList("test"), es.getErrorValue("test"));
    }

    @Test
    public void testWithRelation() {
        ErrorSet es1 = ErrorSet.fromError("foo", "bad");
        ErrorSet es2 = ErrorSet.fromError("bar", "baz");
        ErrorSet es3 = es1.withRelation("rel", es2);
        assertEquals(Lists.newArrayList(es2), es3.getRelations("rel"));
    }

    @Test
    public void testWithRelations() {
        ErrorSet es1 = ErrorSet.fromError("foo", "bad");
        ErrorSet es2 = ErrorSet.fromError("bar", "baz");
        ImmutableMultimap<String,ErrorSet> rel
                = ImmutableMap.of("rel", es2).asMultimap();
        ErrorSet es3 = es1.withRelations(rel);
        assertEquals(Lists.newArrayList(es2), es3.getRelations("rel"));
    }

    @Test
    public void testWithKeyedRelations() {
        ErrorSet es1 = ErrorSet.fromError("foo", "bad");
        ErrorSet es2 = ErrorSet.fromError("bar", "baz");
        ErrorSet es3 = es1.withRelations("rel", Lists.newArrayList(es2));
        assertEquals(Lists.newArrayList(es2), es3.getRelations("rel"));
    }

    @Test
    public void testIsEmpty() throws Exception {
        ErrorSet es = new ErrorSet();
        assertTrue(es.isEmpty());
        ErrorSet es2 = new ErrorSet();
        ErrorSet es3 = es.withRelation("rel", es2);
        assertTrue(es3.isEmpty());
        ErrorSet es4 = ErrorSet.fromError("foo", "bad");
        assertFalse(es4.isEmpty());
        ErrorSet es5 = es.withRelation("rel2", es4);
        assertFalse(es5.isEmpty());
    }
}
