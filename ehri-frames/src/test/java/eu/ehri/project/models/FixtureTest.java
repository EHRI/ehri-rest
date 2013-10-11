package eu.ehri.project.models;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.test.ModelTestBase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
