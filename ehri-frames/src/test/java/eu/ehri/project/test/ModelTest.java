package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.Address;
import eu.ehri.project.models.AgentDescription;
import eu.ehri.project.models.Authority;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.base.Accessor;

public class ModelTest extends ModelTestBase {
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
        assertTrue(unit.getAgent() != null);
        // and have a description
        assertFalse(toList(unit.getDescriptions()).isEmpty());
    }

    @Test
    public void testCollectionNameAccess() throws ItemNotFound {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        Authority a2 = manager.getFrame("a2", Authority.class);
        assertTrue(toList(c1.getNameAccess()).contains(a2));

        // The same should be true backwards
        assertTrue(toList(a2.getMentionedIn()).contains(c1));
    }

    @Test
    public void testRepository() throws ItemNotFound {
        AgentDescription rd1 = manager.getFrame("rd1", AgentDescription.class);
        Address ar1 = manager.getFrame("ar1", Address.class);
        // check we have an address
        assertTrue(toList(rd1.getAddresses()).contains(ar1));
    }
}
