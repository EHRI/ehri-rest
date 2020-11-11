/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.test;

import eu.ehri.project.acl.*;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static eu.ehri.project.acl.PermissionType.*;
import static eu.ehri.project.models.EntityClass.DOCUMENTARY_UNIT;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Exercise various aspects of the permission system.
 * TODO: Streamline this stuff and make it more comprehensible.
 */
public class PermissionsTest extends AbstractFixtureTest {

    private UserProfile user;
    private AclManager acl;
    private PermissionUtils viewHelper;

    @Before
    public void createTestUser() throws Exception {
        // Add a new, fresh user with no perms to test with...
        user = new BundleManager(graph).create(Bundle.of(EntityClass.USER_PROFILE,
                        (Map<String, Object>) TestData.getTestUserBundle().get("data")),
                UserProfile.class);
        viewHelper = new PermissionUtils(graph);
        acl = new AclManager(graph);
        System.out.println("Done creating test user...");
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithBadPerms() throws Exception {
        assertNotNull(api(user).create(Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class));
    }

    @Test
    public void testCreateAsUserWithNewPerms() throws Exception {
        acl.grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user
        );
        assertNotNull(api(user).create(Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithBadScopedPerms() throws Exception {
        Repository scope = manager.getEntity("r1", Repository.class);
        new AclManager(graph, scope).grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user
        );
        assertNotNull(api(user).withScope(SystemScope.getInstance()).create(
                Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class));
    }

    @Test
    public void testCreateAsUserWithGoodScopedPerms() throws Exception {
        Repository scope = manager.getEntity("r1", Repository.class);
        new AclManager(graph, scope).grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user
        );
        assertNotNull(api(user).withScope(scope).create(Bundle.fromData(TestData.getTestDocBundle()),
                DocumentaryUnit.class));
    }

    @Test
    public void testCreateAsUserWithGoodNestedScopedPerms()
            throws Exception {
        Repository scope = manager.getEntity("r1", Repository.class);
        new AclManager(graph, scope).grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user
        );
        DocumentaryUnit c1 = api(user).withScope(scope).create(
                Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class);
        // We should be able to create another item with c1 as the scope,
        // and inherit the perms from r1
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle())
                .withDataValue(Ontology.IDENTIFIER_KEY, "some-other-id");
        DocumentaryUnit c2 = api(user).withScope(c1).create(bundle, DocumentaryUnit.class);
        assertNotNull(c2);
    }

    @Test
    public void testCreateAsUserWithGoodDoubleNestedScopedPerms()
            throws Exception {
        // Same as above, but with the repository as the scope instead of the item.
        Repository r1 = manager.getEntity("r1", Repository.class);
        new AclManager(graph, r1).grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user);
        api(user).withScope(r1).create(
                Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class);
        // We should be able to create another item with c1 as the r1,
        // and inherit the perms from r1
        Bundle bundle = Bundle.fromData(TestData.getTestDocBundle())
                .withDataValue(Ontology.IDENTIFIER_KEY, "some-id");
        DocumentaryUnit c2 = api(user).withScope(r1).create(bundle, DocumentaryUnit.class);
        assertNotNull(c2);
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithDifferentScopedPerms()
            throws Exception {
        Repository scope = manager.getEntity("r1", Repository.class);
        Repository badScope = manager.getEntity("r2", Repository.class);
        acl.withScope(scope).grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user);
        assertNotNull(api(user).withScope(badScope).create(
                Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithDifferentPerms() throws Exception {
        acl.grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), DELETE, user);
        assertNotNull(api(user).create(Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class));
    }

    @Test
    public void testDeleteAsUserWithGoodPerms() throws Exception {
        acl.grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user);
        acl.grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), DELETE, user);
        DocumentaryUnit unit = api(user).create(Bundle.fromData(TestData.getTestDocBundle()),
                DocumentaryUnit.class);
        assertNotNull(unit);
        api(user).delete(unit.getId());
    }

    @Test
    public void testCreateDeleteAsUserWithOwnerPerms() throws Exception {
        acl.grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), OWNER, user);
        DocumentaryUnit unit = api(user).create(Bundle.fromData(TestData.getTestDocBundle()),
                DocumentaryUnit.class);
        assertNotNull(unit);
        api(user).delete(unit.getId());
    }

    @Test
    public void testCreateDeleteAsCreator() throws Exception {
        acl.grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user);
        DocumentaryUnit unit = api(user).create(Bundle.fromData(TestData.getTestDocBundle()),
                DocumentaryUnit.class);
        assertNotNull(unit);
        // Since we created with item, we should have OWNER perms and
        // be able to delete it.
        api(user).delete(unit.getId());
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateDeleteAsUserWithWrongPerms() throws Exception {
        acl.grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), ANNOTATE, user);
        DocumentaryUnit unit = api(user).create(Bundle.fromData(TestData.getTestDocBundle()),
                DocumentaryUnit.class);
        assertNotNull(unit);
        // Revoke my owner perms...
        acl.revokePermission(unit, PermissionType.OWNER, user);
        // This should now throw an error.
        api(user).delete(unit.getId());
    }

    @Test(expected = ValidationError.class)
    public void testCreateWithoutRevoke() throws Exception {
        acl.grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user);
        DocumentaryUnit unit = api(user).create(Bundle.fromData(TestData.getTestDocBundle()),
                DocumentaryUnit.class);
        assertNotNull(unit);
        // Should throw an integrity error
        api(user).create(Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class);
        fail();
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserThenRevoke() throws Exception {
        acl.grantPermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user);
        DocumentaryUnit unit = api(user).create(Bundle.fromData(TestData.getTestDocBundle()),
                DocumentaryUnit.class);
        assertNotNull(unit);
        acl.revokePermission(viewHelper.getContentTypeNode(DOCUMENTARY_UNIT), CREATE, user
        );
        // Should throw an error
        api(user).create(Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class);
        fail();
    }

    @Test
    public void testSetPermissionMatrix() throws Exception {

        GlobalPermissionSet matrix = GlobalPermissionSet.newBuilder()
                .set(ContentTypes.DOCUMENTARY_UNIT, CREATE, DELETE)
                .build();

        try {
            api(user).create(Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class);
            fail();
        } catch (PermissionDenied e) {
            acl.setPermissionMatrix(user, matrix);
            DocumentaryUnit unit = api(user).create(
                    Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class);
            assertNotNull(unit);
            api(user).delete(unit.getId());
        }
    }

    @Test
    public void testSetScopedPermissionMatrix() throws Exception {
        Repository scope = manager.getEntity("r1", Repository.class);

        GlobalPermissionSet matrix = GlobalPermissionSet.newBuilder()
                .set(ContentTypes.DOCUMENTARY_UNIT, CREATE, DELETE)
                .build();

        try {
            api(user).withScope(scope)
                    .create(Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class);
            fail("Should be unable to create an item with scope: " + scope);
        } catch (PermissionDenied e) {
            acl.withScope(scope).setPermissionMatrix(user, matrix);

            try {
                api(user).create(Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class);
                fail("Should be unable to create an item with no scope after setting scoped perms.");
            } catch (PermissionDenied e1) {
                DocumentaryUnit unit = api(user).withScope(scope).create(
                        Bundle.fromData(TestData.getTestDocBundle()), DocumentaryUnit.class);
                assertNotNull(unit);
                api(user).withScope(scope).delete(unit.getId());
            }
        }
    }
}
