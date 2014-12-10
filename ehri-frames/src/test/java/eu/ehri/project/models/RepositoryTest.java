package eu.ehri.project.models;

import com.google.common.collect.Iterables;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class RepositoryTest extends AbstractFixtureTest {
    @Test
    public void testGetCollections() throws Exception {
        Repository r1 = manager.getFrame("r1", Repository.class);
        assertEquals(3L, Iterables.size(r1.getCollections()));

        // Check the cached size
        assertEquals(3L, r1.getChildCount());
    }

    @Test
    public void testRepositoryCanGetAllCollections() throws ItemNotFound {
        Repository agent = manager.getFrame("r1", Repository.class);
        assertEquals(3, Iterables.size(agent.getCollections()));
        assertEquals(5, Iterables.size(agent.getAllCollections()));

    }

    @Test
    public void testRepositoryDescription() throws ItemNotFound {
        RepositoryDescription rd1 = manager.getFrame("rd1", RepositoryDescription.class);
        Address ar1 = manager.getFrame("ar1", Address.class);
        // check we have an address
        assertTrue(toList(rd1.getAddresses()).contains(ar1));
    }
}
