package eu.ehri.project.persistance;

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
    @Test
    public void testToData() throws Exception {
        ErrorSet es = ErrorSet.fromError("foo", "bad");
        Map<String,Object> esdata = es.toData();
        System.out.println(es.toJson());
        assertEquals("bad", ((Map<String,List<String>>)esdata.get(ErrorSet.ERROR_KEY)).get("foo").get(0));
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
