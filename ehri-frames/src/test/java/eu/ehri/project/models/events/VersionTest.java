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
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.*;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class VersionTest extends AbstractFixtureTest {
    private ActionManager actionManager;
    private BundleDAO bundleDAO;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        actionManager = new ActionManager(graph);
        bundleDAO = new BundleDAO(graph);
    }

    @Test
    public void testVersioning() throws Exception {
        Bundle userBundle = Bundle.fromData(TestData.getTestUserBundle());
        Bundle userBundle2 = userBundle.withDataValue("foo", "bar1");
        Bundle userBundle3 = userBundle.withDataValue("foo", "bar2");
        UserProfile user = bundleDAO.create(userBundle, UserProfile.class);

        ActionManager.EventContext ctx1 = actionManager.newEventContext(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.creation);
        SystemEvent first = ctx1.commit();
        assertEquals(1, Iterables.count(first.getSubjects()));

        // Change the user twice...
        ActionManager.EventContext ctx2 = actionManager.newEventContext(user,
                graph.frame(validUser.asVertex(), Actioner.class),
                EventTypes.modification)
                .createVersion(user);
        SystemEvent second = ctx2.commit();
        Mutation<UserProfile> update1 = bundleDAO
                .update(userBundle2, UserProfile.class);
        assertEquals(MutationState.UPDATED, update1.getState());

        SystemEvent third = ctx2.createVersion(user).commit();
        bundleDAO.update(userBundle3, UserProfile.class);
        assertEquals(MutationState.UPDATED, update1.getState());

        // Now compare the values in the versions
        Version firstVersion = second.getPriorVersions().iterator().next();
        assertEquals(second, firstVersion.getTriggeringEvent());
        assertNotNull(firstVersion);
        assertNotNull(firstVersion.getEntity());
        assertEquals(user, firstVersion.getEntity());
        assertEquals(user.getId(), firstVersion.getEntityId());
        assertEquals(user.getType(), firstVersion.getEntityType());
        Bundle firstVersionData = Bundle.fromString(firstVersion.getEntityData());
        // First version doesn't have 'foo' set
        assertNull(firstVersionData.getDataValue("foo"));

        // Now do exactly the same for the second update...
        Version secondVersion = third.getPriorVersions().iterator().next();
        assertNotNull(secondVersion);
        assertNotNull(secondVersion.getEntity());
        assertEquals(user, secondVersion.getEntity());
        assertEquals(user.getId(), secondVersion.getEntityId());
        assertEquals(user.getType(), secondVersion.getEntityType());
        Bundle secondVersionData = Bundle.fromString(secondVersion.getEntityData());
        assertEquals("bar1", secondVersionData.getDataValue("foo"));
    }
}
