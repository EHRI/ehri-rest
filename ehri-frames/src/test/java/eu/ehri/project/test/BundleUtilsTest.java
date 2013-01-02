package eu.ehri.project.test;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.utils.BundleUtils;

public class BundleUtilsTest extends AbstractFixtureTest {

    @Test
    public void testGetPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        assertEquals(TEST_COLLECTION_NAME,
                BundleUtils.get(bundle, Accessor.NAME));
        assertEquals("en", BundleUtils.get(bundle, "describes[0]/languageCode"));
    }

    @Test(expected = BundleUtils.BundlePathError.class)
    public void testGetPathWithBadPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        assertEquals("en",
                BundleUtils.get(bundle, "idontexist[0]/languageCode"));
    }

    @Test(expected = BundleUtils.BundleIndexError.class)
    public void testGetPathWithBadIndex() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        assertEquals("en",
                BundleUtils.get(bundle, "describes[2]/languageCode"));
    }
    
    @Test
    public void testUpdatePath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        Bundle newBundle = BundleUtils.set(bundle, "describes[0]/languageCode",
                "fr");
        assertEquals("fr",
                BundleUtils.get(newBundle, "describes[0]/languageCode"));
    }

    @Test(expected = BundleUtils.BundlePathError.class)
    public void testUpdatePathWithBadPath() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        BundleUtils.set(bundle, "idontexist[0]/languageCode", "fr");
    }
    
    @Test(expected = BundleUtils.BundleIndexError.class)
    public void testUpdatePathWithBadIndex() throws Exception {
        Bundle bundle = Bundle.fromData(getTestBundle());
        BundleUtils.set(bundle, "describes[2]/languageCode", "fr");
    }
}
