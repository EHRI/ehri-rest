package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.views.ViewHelper;
import eu.ehri.project.views.impl.CrudViews;

public class PermissionsTest extends AbstractFixtureTest {

    private UserProfile user;
    private AclManager acl;
    private ViewHelper viewHelper;

    @SuppressWarnings("unchecked")
    @Before
    public void createTestUser() throws ValidationError, IntegrityError, ItemNotFound {
        // Add a new, fresh user with no perms to test with...
        user = new BundleDAO<UserProfile>(graph)
                .create(new BundleFactory<UserProfile>().buildBundle(
                        (Map<String, Object>) getTestUserBundle().get("data"),
                        UserProfile.class));
        views = new CrudViews<DocumentaryUnit>(graph, DocumentaryUnit.class,
                manager.getFrame("r1", Agent.class));
        viewHelper = new ViewHelper(graph, DocumentaryUnit.class);
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
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.CREATE));
        assertNotNull(views.create(getTestBundle(), user));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithBadScopedPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError, ItemNotFound {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.CREATE),
                manager.getFrame("r1", Agent.class));
        assertNotNull(views.setScope(SystemScope.getInstance()).create(getTestBundle(),
                user));
    }

    @Test
    public void testCreateAsUserWithGoodScopedPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError, ItemNotFound {
        Agent scope = manager.getFrame("r1", Agent.class);
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.CREATE));
        assertNotNull(views.setScope(scope).create(getTestBundle(), user));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithDifferentScopedPerms()
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError, ItemNotFound {
        Agent scope = manager.getFrame("r1", Agent.class);
        Agent badScope = manager.getFrame("r2", Agent.class);
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.CREATE), scope);
        assertNotNull(views.setScope(badScope).create(getTestBundle(), user));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithDifferentPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.DELETE));
        assertNotNull(views.create(getTestBundle(), user));
    }

    @Test(expected = PermissionDenied.class)
    public void testDeleteAsUserWithDifferentPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.CREATE));
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        views.delete(unit, user);
    }

    @Test
    public void testDeleteAsUserWithGoodPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.CREATE));
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.DELETE));
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        views.delete(unit, user);
    }

    @Test
    public void testCreateDeleteAsUserWithOwnerPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.OWNER));
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        views.delete(unit, user);
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateDeleteAsUserWithWrongPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.ANNOTATE));
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        views.delete(unit, user);
    }

    @Test(expected = IntegrityError.class)
    public void testCreateWithoutRevoke() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.CREATE));
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
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.CREATE));
        DocumentaryUnit unit = views.create(getTestBundle(), user);
        assertNotNull(unit);
        acl.revokePermissions(user,
                viewHelper.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                viewHelper.getPermission(PermissionTypes.CREATE));
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
        Map<String,List<String>> matrix = new HashMap<String, List<String>>() {{
            put(EntityTypes.DOCUMENTARY_UNIT, new LinkedList<String>() {{
                add(PermissionTypes.CREATE);
                add(PermissionTypes.DELETE);
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
