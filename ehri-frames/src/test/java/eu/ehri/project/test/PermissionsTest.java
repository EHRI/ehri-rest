package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.views.Views;

public class PermissionsTest extends AbstractFixtureTest {

    private UserProfile user;
    private AclManager acl;

    @SuppressWarnings("unchecked")
    @Before
    public void createTestUser() throws ValidationError, IntegrityError {
        // Add a new, fresh user with no perms to test with...
        user = new BundleDAO<UserProfile>(graph)
                .create(new BundleFactory<UserProfile>().buildBundle(
                        (Map<String, Object>) getTestUserBundle().get("data"),
                        UserProfile.class));
        views = new Views<DocumentaryUnit>(graph, DocumentaryUnit.class);
        acl = new AclManager(graph);
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithBadPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        assertNotNull(views.create(getTestBundle(), (Long) user.asVertex()
                .getId()));
    }

    @Test
    public void testCreateAsUserWithNewPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE));
        assertNotNull(views.create(getTestBundle(), (Long) user.asVertex()
                .getId()));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithBadScopedPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE),
                helper.getTestFrame("r1", Agent.class));
        assertNotNull(views.create(getTestBundle(), (Long) user.asVertex()
                .getId()));
    }

    @Test
    public void testCreateAsUserWithGoodScopedPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        Agent scope = helper.getTestFrame("r1", Agent.class);
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE));
        views.setScope(scope);
        assertNotNull(views.create(getTestBundle(), (Long) user.asVertex()
                .getId()));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithDifferentScopedPerms()
            throws PermissionDenied, ValidationError, DeserializationError,
            IntegrityError {
        Agent scope = helper.getTestFrame("r1", Agent.class);
        Agent badScope = helper.getTestFrame("r2", Agent.class);
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE), scope);
        views.setScope(badScope);
        assertNotNull(views.create(getTestBundle(), (Long) user.asVertex()
                .getId()));
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserWithDifferentPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.DELETE));
        assertNotNull(views.create(getTestBundle(), (Long) user.asVertex()
                .getId()));
    }

    @Test(expected = PermissionDenied.class)
    public void testDeleteAsUserWithDifferentPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE));
        DocumentaryUnit unit = views.create(getTestBundle(), (Long) user
                .asVertex().getId());
        assertNotNull(unit);
        views.delete((Long) unit.asVertex().getId(), (Long) user.asVertex()
                .getId());
    }

    @Test
    public void testDeleteAsUserWithGoodPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE));
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.DELETE));
        DocumentaryUnit unit = views.create(getTestBundle(), (Long) user
                .asVertex().getId());
        assertNotNull(unit);
        views.delete((Long) unit.asVertex().getId(), (Long) user.asVertex()
                .getId());
    }

    @Test
    public void testCreateDeleteAsUserWithOwnerPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.OWNER));
        DocumentaryUnit unit = views.create(getTestBundle(), (Long) user
                .asVertex().getId());
        assertNotNull(unit);
        views.delete((Long) unit.asVertex().getId(), (Long) user.asVertex()
                .getId());
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateDeleteAsUserWithWrongPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.ANNOTATE));
        DocumentaryUnit unit = views.create(getTestBundle(), (Long) user
                .asVertex().getId());
        assertNotNull(unit);
        views.delete((Long) unit.asVertex().getId(), (Long) user.asVertex()
                .getId());
    }

    @Test(expected = IntegrityError.class)
    public void testCreateWithoutRevoke() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE));
        DocumentaryUnit unit = views.create(getTestBundle(), (Long) user
                .asVertex().getId());
        assertNotNull(unit);
        // Should throw an integrity error
        views.create(getTestBundle(), (Long) user.asVertex().getId());
        fail();
    }

    @Test(expected = PermissionDenied.class)
    public void testCreateAsUserThenRevoke() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE));
        DocumentaryUnit unit = views.create(getTestBundle(), (Long) user
                .asVertex().getId());
        assertNotNull(unit);
        acl.revokePermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE));
        // Should throw an error
        views.create(getTestBundle(), (Long) user.asVertex().getId());
        fail();
    }

    @SuppressWarnings("serial")
    @Test
    public void testSetPermissionMatrix() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError,
            SerializationError {

        // @formatter:off
        Map<String,Map<String,Boolean>> matrix = new HashMap<String, Map<String,Boolean>>() {{
            put(EntityTypes.DOCUMENTARY_UNIT, new HashMap<String,Boolean>() {{
                put(PermissionTypes.CREATE, true);
                put(PermissionTypes.DELETE, true);
            }});
        }};
        // @formatter:on

        try {
            views.create(getTestBundle(), (Long) user.asVertex().getId());
            fail();
        } catch (PermissionDenied e) {
            acl.setGlobalPermissionMatrix(user, matrix);
            DocumentaryUnit unit = views.create(getTestBundle(), (Long) user
                    .asVertex().getId());
            assertNotNull(unit);
            views.delete((Long) unit.asVertex().getId(), (Long) user.asVertex()
                    .getId());
        }
    }
}
