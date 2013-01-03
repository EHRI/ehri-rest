package eu.ehri.project.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.views.ViewHelper;
import eu.ehri.project.views.impl.CrudViews;

public class PermissionsTest extends AbstractFixtureTest {

    private UserProfile user;
    private AclManager acl;
    private ViewHelper viewHelper;

    @SuppressWarnings("unchecked")
    @Before
    public void createTestUser() throws ValidationError, IntegrityError,
            ItemNotFound {
        // Add a new, fresh user with no perms to test with...
        user = new BundleDAO(graph).create(new Bundle(EntityClass.USER_PROFILE,
                (Map<String, Object>) getTestUserBundle().get("data")),
                UserProfile.class);
        views = new CrudViews<DocumentaryUnit>(graph, DocumentaryUnit.class,
                manager.getFrame("r1", Agent.class));
        viewHelper = new ViewHelper(graph);
        acl = new AclManager(graph);
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithBadPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        assertNotNull(views.create(getTestBundle(), user));
    }

    @Test
    public void testCreateAsUserWithNewPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.CREATE);
        assertNotNull(views.create(getTestBundle(), user));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithBadScopedPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError, ItemNotFound {
        Agent scope = manager.getFrame("r1", Agent.class);
        new AclManager(graph, scope).grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.CREATE);
        assertNotNull(views.setScope(SystemScope.getInstance()).create(
                getTestBundle(), user));
    }

    @Test
    public void testCreateAsUserWithGoodScopedPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError, ItemNotFound {
        Agent scope = manager.getFrame("r1", Agent.class);
        new AclManager(graph, scope).grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.CREATE);
        assertNotNull(views.setScope(scope).create(getTestBundle(), user));
    }

    @Test
    public void testCreateAsUserWithGoodNestedScopedPerms()
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError, ItemNotFound {
        Agent scope = manager.getFrame("r1", Agent.class);
        new AclManager(graph, scope).grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.CREATE);
        DocumentaryUnit c1 = views.setScope(scope)
                .create(getTestBundle(), user);
        // We have to explicitly set the scope of this new item.
        c1.setScope(scope);
        // We should be able to create another item with c1 as the scope,
        // and inherit the perms from r1
        // FIXME: We have to alter the test data so it doesn't throw a
        // validation error due to duplicate identifiers
        Bundle bundle = Bundle.fromData(getTestBundle()).withDataValue(
                AccessibleEntity.IDENTIFIER_KEY, "nested-item");
        DocumentaryUnit c2 = views.setScope(c1).create(bundle.toData(), user);
        assertNotNull(c2);
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithDifferentScopedPerms()
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError, ItemNotFound {
        Agent scope = manager.getFrame("r1", Agent.class);
        Agent badScope = manager.getFrame("r2", Agent.class);
        new AclManager(graph, scope).grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.CREATE);
        assertNotNull(views.setScope(badScope).create(getTestBundle(), user));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithDifferentPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.DELETE);
        assertNotNull(views.create(getTestBundle(), user));
    }

    @Test
    public void testDeleteAsUserWithGoodPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.CREATE);
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.DELETE);
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        views.delete(unit, user);
    }

    @Test
    public void testCreateDeleteAsUserWithOwnerPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.OWNER);
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        views.delete(unit, user);
    }

    @Test
    public void testCreateDeleteAsCreator() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.CREATE);
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        // Since we created with item, we should have OWNER perms and
        // be able to delete it.
        views.delete(unit, user);
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateDeleteAsUserWithWrongPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.ANNOTATE);
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        // Revoke my owner perms...
        acl.revokePermissions(user, unit, PermissionType.OWNER);
        // This should now throw an error.
        views.delete(unit, user);
    }

    @Test(expected = ValidationError.class)
    public void testCreateWithoutRevoke() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.CREATE);
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        // Should throw an integrity error
        views.create(getTestBundle(), user);
        fail();
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserThenRevoke() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.CREATE);
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        acl.revokePermissions(user,
                viewHelper.getContentType(EntityClass.DOCUMENTARY_UNIT),
                PermissionType.CREATE);
        // Should throw an error
        views.create(getTestBundle(), user);
        fail();
    }

    @SuppressWarnings("serial")
    @Test
    public void testSetPermissionMatrix() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {

        // @formatter:off
        Map<ContentTypes,List<PermissionType>> matrix = new HashMap<ContentTypes, List<PermissionType>>() {{
            put(ContentTypes.DOCUMENTARY_UNIT, new LinkedList<PermissionType>() {{
                add(PermissionType.CREATE);
                add(PermissionType.DELETE);
            }});
        }};
        // @formatter:on

        try {
            views.create(getTestBundle(), user);
            fail();
        } catch (PermissionDenied e) {
            acl.setGlobalPermissionMatrix(user, matrix);
            DocumentaryUnit unit = views.create(getTestBundle(), user);
            assertNotNull(unit);
            views.delete(unit, user);
        }
    }
}
