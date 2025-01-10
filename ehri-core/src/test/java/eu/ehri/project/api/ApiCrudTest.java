/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.api;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.HierarchyError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class ApiCrudTest extends AbstractFixtureTest {

    @Test
    public void testDetail() throws ItemNotFound {
        DocumentaryUnit unit = api(adminUser).get(item.getId(), DocumentaryUnit.class);
        assertEquals(item.asVertex(), unit.asVertex());
    }

    @Test
    public void testUserProfile() throws ItemNotFound {
        UserProfile user = api(adminUser).get(adminUser.getId(), UserProfile.class);
        assertEquals(adminUser.asVertex(), user.asVertex());
    }

    @Test(expected = ItemNotFound.class)
    public void testDetailAnonymous() throws ItemNotFound {
        anonApi().get(item.getId(), DocumentaryUnit.class);
    }

    @Test(expected = ItemNotFound.class)
    public void testDetailPermissionDenied() throws ItemNotFound {
        api(basicUser).get(item.getId(), DocumentaryUnit.class);
    }

    @Test
    public void testCreate() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = api(adminUser).create(bundle, DocumentaryUnit.class);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUnauthorized() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = api(basicUser).create(bundle, DocumentaryUnit.class);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));
    }

    @Test
    public void testCreateAsUnauthorizedAndThenGrant() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());

        try {
            api(basicUser).create(bundle, DocumentaryUnit.class);
            fail("Creation should throw "
                    + PermissionDenied.class.getSimpleName());
        } catch (PermissionDenied e) {
            // We expected that permission denied... now explicitly add
            // permissions.
            PermissionGrantTarget target = manager.getEntity(
                    ContentTypes.DOCUMENTARY_UNIT.getName(),
                    PermissionGrantTarget.class);
            new AclManager(graph).grantPermission(target, PermissionType.CREATE, basicUser
            );
            DocumentaryUnit unit = api(basicUser).create(bundle, DocumentaryUnit.class);
            assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));
        }
    }

    @Test
    public void testCreateWithScope() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        // In the fixtures, 'reto' should have a grant for 'CREATE'
        // scoped to the 'r1' repository.
        DocumentaryUnit unit = api(basicUser)
                .withScope(r1).create(bundle, DocumentaryUnit.class);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));
    }

    @Test
    public void testUserDetailAccessDenied() throws ItemNotFound {
        api(basicUser).get(adminUser.getId(), UserProfile.class);
    }

    @Test
    public void testUpdate() throws Exception {
        Repository repository = manager.getEntity("r1", Repository.class);
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle());
        DocumentaryUnit unit = api(adminUser).withScope(repository)
                .create(bundle, DocumentaryUnit.class);
        assertEquals(TestData.TEST_COLLECTION_NAME, unit.getProperty("name"));

        String newName = TestData.TEST_COLLECTION_NAME + " with new stuff";
        Bundle newBundle = bundle.withId(unit.getId()).withDataValue(
                "name", newName);

        DocumentaryUnit changedUnit = api(adminUser)
                .update(newBundle, DocumentaryUnit.class).getNode();
        assertEquals(newName, changedUnit.getProperty("name"));
        DocumentaryUnitDescription desc = graph.frame(
                changedUnit.getDescriptions().iterator().next().asVertex(),
                DocumentaryUnitDescription.class);
        assertTrue(manager.exists("nl-r1-someid_01.eng-someid_01"));

        // Add a new description and check ids etc
        Bundle newDesc = Bundle.of(EntityClass.DOCUMENTARY_UNIT_DESCRIPTION)
                .withDataValue(Ontology.LANGUAGE_OF_DESCRIPTION, "ang")
                .withDataValue(Ontology.IDENTIFIER_KEY, "Latn")
                .withDataValue(Ontology.NAME_KEY, "Test Desc 2");
        DocumentaryUnit changedUnit2 = api(adminUser)
                .update(newBundle
                        .withRelation(Ontology.DESCRIPTION_FOR_ENTITY, newDesc
                        ), DocumentaryUnit.class).getNode();
        assertTrue(manager.exists("nl-r1-someid_01.ang-latn"));

        // Check the nested item was created correctly
        DatePeriod datePeriod = desc.getDatePeriods().iterator().next();
        assertNotNull(datePeriod);
        assertEquals(TestData.TEST_START_DATE, datePeriod.getStartDate());

        // And that the reverse relationship works.
        assertEquals(desc.asVertex(), datePeriod.getEntity().asVertex());
    }

    @Test
    public void testUserUpdate() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestUserBundle());
        UserProfile user = api(adminUser).create(bundle, UserProfile.class);
        assertEquals(TestData.TEST_USER_NAME, user.getName());

        String newName = TestData.TEST_USER_NAME + " with new stuff";
        Bundle newBundle = bundle.withId(user.getId()).withDataValue(
                "name", newName);
        UserProfile changedUser = api(adminUser).update(newBundle, UserProfile.class).getNode();
        assertEquals(newName, changedUser.getName());
    }

    @Test
    public void testUserCreate() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestUserBundle());
        UserProfile user = api(adminUser).create(bundle, UserProfile.class);
        assertEquals(TestData.TEST_USER_NAME, user.getName());
    }

    @Test
    public void testGroupCreate() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestGroupBundle());
        Group group = api(adminUser).create(bundle, Group.class);
        assertEquals(TestData.TEST_GROUP_NAME, group.getName());
    }

    @Test
    public void testCreateWithError() throws Exception {
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle())
                .removeDataValue("name");

        // This shouldn't barf because the collection does not need a name.
        DocumentaryUnit unit = api(adminUser).create(bundle, DocumentaryUnit.class);
        assertNull(unit.getProperty("name"));
    }

    @Test(expected = HierarchyError.class)
    public void testDeleteWithHierarchyError() throws Exception {
        api(adminUser).delete(item.getId());
    }

    @Test
    public void testDeleteAdmin() throws Exception {
        try {
            api(adminUser).delete(Group.ADMIN_GROUP_IDENTIFIER);
            fail("Should not have been able to delete admin group");
        } catch (PermissionDenied e) {
            assertEquals(Group.ADMIN_GROUP_IDENTIFIER, e.getEntity());
            assertEquals(adminUser.getId(), e.getAccessor());
            assertEquals(PermissionType.DELETE.getName(), e.getPermission());
            assertEquals(SystemScope.getInstance().getId(), e.getScope());
        }
    }

    @Test
    public void testDelete() throws Exception {
        int shouldDelete = 1;

        // FIXME: Surely there's a better way of doing this???
        DocumentaryUnit c4 = api(adminUser).get("c4", DocumentaryUnit.class);
        Iterator<Description> descIter = c4.getDescriptions().iterator();
        for (; descIter.hasNext(); shouldDelete++) {
            DocumentaryUnitDescription d = graph.frame(descIter.next().asVertex(), DocumentaryUnitDescription.class);
            shouldDelete += Iterables.size(d.getDatePeriods());
            shouldDelete += Iterables.size(d.getAccessPoints());
            shouldDelete += Iterables.size(d.getUnknownProperties());
        }

        int deleted = api(adminUser).delete(c4.getId());
        assertEquals(shouldDelete, deleted);
    }

    @Test(expected = HierarchyError.class)
    public void testDeleteChildrenWithError() throws Exception {
        api(adminUser).deleteChildren(item.getId(), false, true, Optional.empty());
    }

    @Test
    public void testDeleteChildren() throws Exception {
        List<String> out = api(adminUser).deleteChildren(item.getId(), true, true, Optional.empty());
        assertEquals(Lists.newArrayList("c2", "c3"), out);
    }

    @Test
    public void testDeleteChildrenWithBatchCallback() throws Exception {
        final List<String> ids = Lists.newArrayList();
        List<String> out = api(adminUser).deleteChildren(item.getId(), true, true, (num, id) -> {
            ids.add(id);
        }, Optional.empty());
        assertEquals(Lists.newArrayList("c2", "c3"), out);
        assertEquals(ids, out);
    }
}
