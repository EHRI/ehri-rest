package eu.ehri.project.persistence;

import com.google.common.collect.Lists;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.persistence.utils.BundleUtils;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *
 * TODO: Cover class more comprehensively.
 */
public class SerializerTest extends AbstractFixtureTest {

    @Test
    public void testNonLiteSerialization() throws Exception {
        DocumentaryUnit doc = manager.getFrame("c1", DocumentaryUnit.class);

        Bundle serialized = new Serializer(graph)
                .vertexFrameToBundle(doc);

        // Name of repository should be serialized
        assertEquals("Documentary Unit 1",
                BundleUtils.get(serialized, "describes[0]/name"));
        assertNotNull(BundleUtils.get(serialized, "describes[0]/scopeAndContent"));
        System.out.println(serialized);
        assertEquals("NIOD Description",
                BundleUtils.get(serialized, "heldBy[0]/describes[0]/name"));

        // But the address data shouldn't
        try {
            BundleUtils.get(serialized, "heldBy[0]/describes[0]/hasAddress[0]/streetAddress");
            fail("Default serializer should not serialize addresses in repository descriptions");
        } catch (BundleUtils.BundlePathError e) {
        }

    }

    @Test
    public void testLiteSerialization() throws Exception {
        DocumentaryUnit doc = manager.getFrame("c1", DocumentaryUnit.class);

        Bundle serialized = new Serializer.Builder(graph).withLiteMode(true).build()
                .vertexFrameToBundle(doc);

        // Name of doc and repository should be serialized
        assertEquals("Documentary Unit 1",
                BundleUtils.get(serialized, "describes[0]/name"));
        // Not mandatory properties should be null...
        assertNull(BundleUtils.get(serialized, "describes[0]/scopeAndContent"));

        assertEquals("NIOD Description",
                BundleUtils.get(serialized, "heldBy[0]/describes[0]/name"));
    }

    @Test
    public void testIncludedProperties() throws Exception {
        DocumentaryUnit doc = manager.getFrame("c1", DocumentaryUnit.class);

        Serializer serializer = new Serializer.Builder(graph).withLiteMode(true).build();
        Bundle serialized = serializer.vertexFrameToBundle(doc);

        // Name of doc and repository should be serialized
        assertEquals("Documentary Unit 1", BundleUtils.get(serialized, "describes[0]/name"));
        // Not mandatory properties should be null...
        assertNull(BundleUtils.get(serialized, "describes[0]/scopeAndContent"));

        Bundle serialized2 = serializer.withIncludedProperties(Lists.newArrayList("scopeAndContent"))
                .vertexFrameToBundle(doc);
        assertNotNull(BundleUtils.get(serialized2, "describes[0]/scopeAndContent"));
    }
}
