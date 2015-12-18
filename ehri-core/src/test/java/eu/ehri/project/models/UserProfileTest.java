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

package eu.ehri.project.models;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: mikebryant
 */
public class UserProfileTest extends AbstractFixtureTest {

    private UserProfile mike;
    private UserProfile reto;
    private UserProfile linda;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        reto = manager.getFrame("reto", UserProfile.class);
        mike = manager.getFrame("mike", UserProfile.class);
        linda = manager.getFrame("linda", UserProfile.class);
    }

    @Test
    public void testGetGroups() throws Exception {
        assertTrue(mike.getGroups().iterator().hasNext());
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        assertTrue(Iterables.contains(mike.getGroups(), admin));
    }

    @Test
    public void testCoGroupMembers() throws Exception {
        // mike is in groups kcl and admin. reto is in
        // group kcl and veerle in admin. Therefore mike's
        // co-group members are reto and veerle...
        UserProfile veerle = manager.getFrame("veerle", UserProfile.class);
        List<UserProfile> profiles = Lists.newArrayList(mike.coGroupMembers());
        assertEquals(2, profiles.size());
        assertTrue(profiles.contains(reto));
        assertTrue(profiles.contains(veerle));
    }

    @Test
    public void testFollowing() throws Exception {
        assertTrue(Iterables.isEmpty(mike.getFollowers()));
        assertTrue(Iterables.isEmpty(linda.getFollowers()));
        assertTrue(Iterables.isEmpty(reto.getFollowing()));

        reto.addFollowing(mike);
        assertEquals(1L, Iterables.size(mike.getFollowers()));
        assertEquals(1L, Iterables.size(reto.getFollowing()));

        reto.addFollowing(linda);
        assertEquals(1L, Iterables.size(mike.getFollowers()));
        assertEquals(2L, Iterables.size(reto.getFollowing()));

        // Get count caching
        assertEquals(2L, reto.getFollowingCount());
        assertEquals(1L, mike.getFollowerCount());
        assertEquals(1L, linda.getFollowerCount());

        assertFalse(Iterables.isEmpty(reto.getFollowing()));
        assertTrue(Iterables.contains(reto.getFollowing(), mike));
        assertTrue(Iterables.contains(reto.getFollowing(), linda));
        reto.removeFollowing(mike);
        assertTrue(Iterables.isEmpty(mike.getFollowers()));
        assertFalse(Iterables.isEmpty(reto.getFollowing()));

        assertEquals(1L, reto.getFollowingCount());
        assertEquals(0L, mike.getFollowerCount());
    }

    @Test
    public void testIsFollowing() throws Exception {
        assertFalse(reto.isFollowing(mike));
        reto.addFollowing(mike);
        assertTrue(reto.isFollowing(mike));
        assertTrue(mike.isFollower(reto));
        assertFalse(mike.isFollowing(reto));
    }

    @Test
    public void testDuplicateWatches() throws Exception {
        assertFalse(reto.isFollowing(mike));
        // Do this twice and ensure the follower count isn't altered...
        reto.addFollowing(mike);
        reto.addFollowing(mike);
        assertEquals(1L, Iterables.size(reto.getFollowing()));
        reto.addFollowing(linda);
        assertEquals(2L, Iterables.size(reto.getFollowing()));
    }

    @Test
    public void testWatching() throws Exception {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        DocumentaryUnit c2 = manager.getFrame("c2", DocumentaryUnit.class);
        assertFalse(mike.isWatching(c1));
        mike.addWatching(c1);
        assertTrue(mike.isWatching(c1));
        assertEquals(1L, mike.getWatchingCount());
        assertTrue(Iterables.contains(c1.getWatchers(), mike));
        assertEquals(1L, c1.getWatchedCount());
        assertTrue(Iterables.contains(mike.getWatching(), c1));

        mike.addWatching(c2);
        assertEquals(2L, mike.getWatchingCount());
        mike.removeWatching(c2);
        assertEquals(1L, mike.getWatchingCount());

        mike.removeWatching(c1);
        assertFalse(mike.isWatching(c1));
        assertEquals(0L, mike.getWatchingCount());
        assertEquals(0L, c1.getWatchedCount());
    }

    @Test
    public void testBlocking() throws Exception {
        mike.addBlocked(reto);
        mike.addBlocked(linda);
        assertTrue(mike.isBlocking(reto));
        assertTrue(mike.isBlocking(linda));
        assertTrue(Iterables.contains(mike.getBlocked(), reto));
        assertTrue(Iterables.contains(mike.getBlocked(), linda));
        mike.removeBlocked(reto);
        assertFalse(Iterables.contains(mike.getBlocked(), reto));
        assertTrue(Iterables.contains(mike.getBlocked(), linda));
    }
}
