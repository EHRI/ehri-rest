/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

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

        ActionManager.EventContext ctx = actionManager.newEventContext(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation);
        SystemEvent first = ctx.commit();
        assertEquals(1, Iterables.count(first.getSubjects()));

        // Delete the user and log it
        ActionManager.EventContext ctx2 = actionManager.newEventContext(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.deletion);
        ctx2.commit();
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

        ActionManager.EventContext ctx = actionManager.newEventContext(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation);
        SystemEvent first = ctx.commit();
        assertEquals(1, Iterables.count(first.getSubjects()));

        // Delete the user and log it
        ActionManager.EventContext ctx2 = actionManager.newEventContext(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.modification);
        SystemEvent second = ctx2.commit();
        bundleDAO.update(userBundle2, UserProfile.class);

        SystemEvent third = ctx2.commit();
        bundleDAO.update(userBundle3, UserProfile.class);

        ActionManager.EventContext ctx3 = actionManager.newEventContext(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.deletion);
        SystemEvent forth = ctx3.commit();

        // creation and modification are different
        assertFalse(sameAs(first, second));

        // two modification events are the same
        assertTrue(sameAs(second, third));

        assertFalse(sameAs(forth, third));

        // Check with another type
        Bundle repoBundle = Bundle.fromData(TestData.getTestAgentBundle());
        Repository repository = bundleDAO.create(repoBundle, Repository.class);

        // Delete the user and log it
        ActionManager.EventContext ctx4 = actionManager.newEventContext(repository,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.modification);
        SystemEvent repoEvent = ctx4.commit();

        assertFalse(sameAs(second, repoEvent));
    }

    @Test
    public void testSameAsWithTimeDiff() throws Exception {
        Bundle userBundle = Bundle.fromData(TestData.getTestUserBundle());
        UserProfile user = bundleDAO.create(userBundle, UserProfile.class);

        ActionManager.EventContext ctx = actionManager.newEventContext(user,
                validUser,
                EventTypes.creation);
        SystemEvent first = ctx.commit();
        assertEquals(1, Iterables.count(first.getSubjects()));

        // Do the same thing again after 1/2 second
        Thread.sleep(500);
        ActionManager.EventContext ctx2 = actionManager.newEventContext(user,
                validUser,
                EventTypes.creation);
        SystemEvent second = ctx2.commit();

        // And again after 1 full second
        Thread.sleep(500);
        ActionManager.EventContext ctx3 = actionManager.newEventContext(user,
                validUser,
                EventTypes.creation);
        SystemEvent third = ctx3.commit();

        // Without considering time difference, first and second events
        // are "the same"
        assertTrue(sameAs(first, second));
        // ... as are first and third
        assertTrue(sameAs(first, third));
        // And they're also the same, with a cutoff time
        // below a second, since they happen in quick
        // succession.
        assertTrue(ActionManager.canAggregate(first, second, 1));
        // ... but with a time diff of less than a second,
        // they are not the same
        assertFalse(ActionManager.canAggregate(first, second, 0));
        // And nor are the first and third events, which
        // are separated by more than 1 second.
        assertFalse(ActionManager.canAggregate(first, third, 1));
    }
}
