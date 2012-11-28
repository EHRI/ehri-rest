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
        List<Group> groups = toList(helper.getTestFrames(EntityTypes.GROUP,
                Group.class));
        assertFalse(groups.isEmpty());

        // Check the first group has a user in it
        List<Accessor> users = toList(groups.get(0).getMembers());
        assertFalse(users.isEmpty());
    }

    @Test
    public void testCollectionHelpByRepo() {
        DocumentaryUnit unit = helper.getTestFrame("c1", DocumentaryUnit.class);
        assertTrue(unit.getAgent() != null);
        // and have a description
        assertFalse(toList(unit.getDescriptions()).isEmpty());
    }

    @Test
    public void testCollectionNameAccess() {
        DocumentaryUnit c1 = helper.getTestFrame("c1", DocumentaryUnit.class);
        Authority a2 = helper.getTestFrame("a2", Authority.class);
        assertTrue(toList(c1.getNameAccess()).contains(a2));

        // The same should be true backwards
        assertTrue(toList(a2.getMentionedIn()).contains(c1));
    }

    @Test
    public void testRepository() {
        AgentDescription rd1 = helper.getTestFrame("rd1", AgentDescription.class);
        Address ar1 = helper.getTestFrame("ar1", Address.class);
        // check we have an address
        assertTrue(toList(rd1.getAddresses()).contains(ar1));
    }
}
