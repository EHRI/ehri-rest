package eu.ehri.project.models;

import java.util.List;

import com.google.common.collect.Iterables;
import eu.ehri.project.test.ModelTestBase;
import org.junit.Test;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.base.Accessor;

import static org.junit.Assert.*;

public class FixtureTest extends ModelTestBase {
    @Test
    public void testTheFixturesLoaded() {
        assertTrue(graph.getVertices().iterator().hasNext());
    }

    @Test
    public void testTheGraphContainsGroups() throws ItemNotFound {
        Iterable<Group> frames = manager.getFrames(EntityClass.GROUP,
                Group.class);
        List<Group> groups = toList(frames);
        assertFalse(groups.isEmpty());

        // Check the admin group has some members
        List<Accessor> users = toList(manager.getFrame("admin", Group.class)
                .getMembers());
        assertFalse(users.isEmpty());
    }

    @Test
    public void testCollectionHelpByRepo() throws ItemNotFound {
        DocumentaryUnit unit = manager.getFrame("c1", DocumentaryUnit.class);
        assertNotNull(unit.getRepository());
        // and have a description
        assertFalse(toList(unit.getDescriptions()).isEmpty());
    }

    @Test
    public void testChildDocsCanAccessTheirAgent() throws ItemNotFound {
        DocumentaryUnit unit = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit child = manager.getFrame("c3", DocumentaryUnit.class);
        assertNotNull(child.getRepository());
        assertNotNull(unit.getRepository());
        assertEquals(unit.getRepository(), child.getRepository());
    }

    @Test
    public void testAgentsCanGetAllCollections() throws ItemNotFound {
        Repository agent = manager.getFrame("r1", Repository.class);
        assertEquals(2, Iterables.size(agent.getCollections()));
        assertEquals(4, Iterables.size(agent.getAllCollections()));

    }

    @Test
    public void testRepository() throws ItemNotFound {
        RepositoryDescription rd1 = manager.getFrame("rd1", RepositoryDescription.class);
        Address ar1 = manager.getFrame("ar1", Address.class);
        // check we have an address
        assertTrue(toList(rd1.getAddresses()).contains(ar1));
    }
}
