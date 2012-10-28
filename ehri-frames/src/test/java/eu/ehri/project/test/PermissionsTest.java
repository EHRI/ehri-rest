package eu.ehri.project.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionTypes;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.IntegrityError;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Agent;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.DatePeriod;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.persistance.BundleDAO;
import eu.ehri.project.persistance.BundleFactory;
import eu.ehri.project.views.ActionViews;
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
    public void setCreateAsUserWithBadPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        views.create(getTestBundle(), (Long) user.asVertex().getId());
    }

    @Test
    public void setCreateAsUserWithNewPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE));
        views.create(getTestBundle(), (Long) user.asVertex().getId());
    }

    @Test(expected = PermissionDenied.class)
    public void setCreateAsUserWithBadScopedPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE),
                helper.getTestFrame("r1", Agent.class));
        views.create(getTestBundle(), (Long) user.asVertex().getId());
    }

    @Test
    public void setCreateAsUserWithGoodScopedPerms() throws PermissionDenied,
            ValidationError, DeserializationError, IntegrityError {
        Agent scope = helper.getTestFrame("r1", Agent.class);
        acl.grantPermissions(user,
                views.getContentType(EntityTypes.DOCUMENTARY_UNIT),
                views.getPermission(PermissionTypes.CREATE));
        views.create(getTestBundle(), (Long) user.asVertex().getId(),
                (Long) scope.asVertex().getId());
    }
}
