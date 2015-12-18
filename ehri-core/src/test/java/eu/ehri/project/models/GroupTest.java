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
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GroupTest extends AbstractFixtureTest {

    @Test
    public void testAdminInitializedProperty() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        // Both identifier and name should initialize to 'admin'
        assertEquals(admin.getIdentifier(), Group.ADMIN_GROUP_IDENTIFIER);
        assertEquals(admin.getName(), Group.ADMIN_GROUP_NAME);
    }

    @Test
    public void testAddMember() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        List<Accessor> list = Lists.newArrayList(admin.getMembers());
        long numMembers = list.size();
        // Adding same member twice should affect member count - it should be idempotent
        admin.addMember(validUser);
        assertEquals(numMembers, Iterables.size(admin.getMembers()));
        admin.addMember(invalidUser);
        assertEquals(numMembers + 1L, Iterables.size(admin.getMembers()));
    }

    @Test
    public void testRemoveMember() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        List<Accessor> list = Lists.newArrayList(admin.getMembers());
        long numMembers = list.size();
        // Adding same member twice should affect member count - it should be idempotent
        admin.removeMember(invalidUser);
        assertEquals(numMembers, Iterables.size(admin.getMembers()));
        admin.removeMember(validUser);
        assertEquals(numMembers - 1L, Iterables.size(admin.getMembers()));
    }

    @Test
    public void testGetAllUserProfileMembers() throws Exception {
        Group admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        // All users should be mike, veerle, tim (inherited)
        List<?> userProfileList = toList(admin.getAllUserProfileMembers());
        assertEquals(3, userProfileList.size());
    }
}
