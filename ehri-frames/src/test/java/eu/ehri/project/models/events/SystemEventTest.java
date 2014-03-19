package eu.ehri.project.models.events;

import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SystemEventTest extends AbstractFixtureTest {
    @Test
    public void testGetSubjects() throws Exception {
        ActionManager am = new ActionManager(graph);
        BundleDAO dao = new BundleDAO(graph);
        Serializer serializer = new Serializer(graph);
        // Create a user and log it
        Bundle userBundle = Bundle.fromData(TestData.getTestUserBundle());
        UserProfile user = dao.create(userBundle, UserProfile.class);

        SystemEvent first = am.logEvent(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation).getSystemEvent();
        assertEquals(1, Iterables.count(first.getSubjects()));

        // Delete the user and log it
        SystemEvent second = am.logEvent(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.deletion).getSystemEvent();
        dao.delete(serializer.vertexFrameToBundle(user));

        // First event should now have 0 subjects, since it's
        // been deleted.
        assertEquals(0, Iterables.count(first.getSubjects()));
    }
}
