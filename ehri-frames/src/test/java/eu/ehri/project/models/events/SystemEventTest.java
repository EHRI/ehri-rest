package eu.ehri.project.models.events;

import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static eu.ehri.project.persistence.ActionManager.sameAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SystemEventTest extends AbstractFixtureTest {

    private ActionManager actionManager;
    private BundleDAO bundleDAO;
    private Serializer serializer;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        actionManager = new ActionManager(graph);
        bundleDAO = new BundleDAO(graph);
        serializer = new Serializer(graph);
    }

    @Test
    public void testGetSubjects() throws Exception {
        Bundle userBundle = Bundle.fromData(TestData.getTestUserBundle());
        UserProfile user = bundleDAO.create(userBundle, UserProfile.class);

        SystemEvent first = actionManager.logEvent(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation).getSystemEvent();
        assertEquals(1, Iterables.count(first.getSubjects()));

        // Delete the user and log it
        actionManager.logEvent(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.deletion).getSystemEvent();
        bundleDAO.delete(serializer.vertexFrameToBundle(user));

        // First event should now have 0 subjects, since it's
        // been deleted.
        assertEquals(0, Iterables.count(first.getSubjects()));
    }

    @Test
    public void testSameAs() throws Exception {
        Bundle userBundle = Bundle.fromData(TestData.getTestUserBundle());
        Bundle userBundle2 = userBundle.withDataValue("foo", "bar1");
        Bundle userBundle3 = userBundle.withDataValue("foo", "bar2");
        UserProfile user = bundleDAO.create(userBundle, UserProfile.class);

        SystemEvent first = actionManager.logEvent(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation).getSystemEvent();
        assertEquals(1, Iterables.count(first.getSubjects()));

        // Delete the user and log it
        SystemEvent second = actionManager.logEvent(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.modification).getSystemEvent();
        bundleDAO.update(userBundle2, UserProfile.class);

        SystemEvent third = actionManager.logEvent(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.modification).getSystemEvent();
        bundleDAO.update(userBundle3, UserProfile.class);

        SystemEvent forth = actionManager.logEvent(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.deletion).getSystemEvent();

        // creation and modification are different
        assertFalse(sameAs(first, second));

        // two modification events are the same
        assertTrue(sameAs(second, third));

        assertFalse(sameAs(forth, third));

        // Check with another type
        Bundle repoBundle = Bundle.fromData(TestData.getTestAgentBundle());
        Repository repository = bundleDAO.create(repoBundle, Repository.class);

        // Delete the user and log it
        SystemEvent repoEvent = actionManager.logEvent(repository,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.modification).getSystemEvent();

        assertFalse(sameAs(second, repoEvent));
    }
}
