package eu.ehri.project.models;

import com.google.common.collect.Iterables;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * User: mikebryant
 */
public class CountryTest extends AbstractFixtureTest {

    @Test
    public void testGetChildCount() throws Exception {
        Country country = manager.getFrame("nl", Country.class);
        Repository repo = new BundleDAO(graph)
                .create(Bundle.fromData(TestData.getTestAgentBundle()), Repository.class);
        country.addRepository(repo);
        // 2 nl repositories in the fixtures, plus the one we just made...
        assertEquals(3L, (long) country.getChildCount());
    }

    @Test
    public void testGetRepositories() throws Exception {
        Country country = manager.getFrame("nl", Country.class);
        assertEquals(2L, Iterables.size(country.getRepositories()));
    }

    @Test
    public void testAddRepository() throws Exception {
        Country country = manager.getFrame("nl", Country.class);
        Repository repo = new BundleDAO(graph)
                .create(Bundle.fromData(TestData.getTestAgentBundle()), Repository.class);
        // Test setting country on repo delegates correctly and
        // increments the country count...
        repo.setCountry(country);
        // 2 nl repositories in the fixtures, plus the one we just made...
        assertEquals(3L, (long) country.getChildCount());
    }


}
