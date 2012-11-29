package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import eu.ehri.project.models.Address;
import eu.ehri.project.models.AgentDescription;
import eu.ehri.project.models.Authority;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.base.Accessor;

public class ModelTest extends ModelTestBase {
    @Test
    public void testTheFixturesLoaded() {
        assertTrue(graph.getVertices().iterator().hasNext());
    }

    @Test
    public void testTheGraphContainsGroups() {
        Iterable<Group> frames = manager.getFrames(EntityTypes.GROUP,
                Group.class);
        List<Group> groups = toList(frames);
        assertFalse(groups.isEmpty());

        // Check the admin group has some members
        List<Accessor> users = toList(manager.frame("admin", Group.class)
                .getMembers());
        assertFalse(users.isEmpty());
    }

    @Test
    public void testCollectionHelpByRepo() {
        DocumentaryUnit unit = manager.frame("c1", DocumentaryUnit.class);
        assertTrue(unit.getAgent() != null);
        // and have a description
        assertFalse(toList(unit.getDescriptions()).isEmpty());
    }

    @Test
    public void testCollectionNameAccess() {
        DocumentaryUnit c1 = manager.frame("c1", DocumentaryUnit.class);
        Authority a2 = manager.frame("a2", Authority.class);
        assertTrue(toList(c1.getNameAccess()).contains(a2));

        // The same should be true backwards
        assertTrue(toList(a2.getMentionedIn()).contains(c1));
    }

    @Test
    public void testRepository() {
        AgentDescription rd1 = manager.frame("rd1", AgentDescription.class);
        Address ar1 = manager.frame("ar1", Address.class);
        // check we have an address
        assertTrue(toList(rd1.getAddresses()).contains(ar1));
    }
}
