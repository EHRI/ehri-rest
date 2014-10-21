package eu.ehri.project.models.base;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.views.Crud;
import eu.ehri.project.views.ViewFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AccessibleEntityTest extends AbstractFixtureTest {
    @Test
    public void testGetAccessors() throws Exception {
        AccessibleEntity c1 = manager.getFrame("c1", AccessibleEntity.class);
        AccessibleEntity admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, AccessibleEntity.class);
        List<Accessor> accessors = Lists.newArrayList(c1.getAccessors());
        assertEquals(2L, accessors.size());
        assertTrue(accessors.contains(validUser)); // mike
        assertTrue(accessors.contains(admin));
    }

    @Test
    public void testAddAccessor() throws Exception {
        AccessibleEntity c1 = manager.getFrame("c1", AccessibleEntity.class);
        Accessor admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Accessor.class);
        List<Accessor> accessors = Lists.newArrayList(c1.getAccessors());
        assertEquals(2L, accessors.size());
        c1.addAccessor(admin);
        assertEquals(2L, Iterables.size(c1.getAccessors())); // same size
        c1.addAccessor(invalidUser);
        assertEquals(3L, Iterables.size(c1.getAccessors()));
    }

    @Test
    public void testRemoveAccessor() throws Exception {
        AccessibleEntity c1 = manager.getFrame("c1", AccessibleEntity.class);
        Accessor admin = manager.getFrame(Group.ADMIN_GROUP_IDENTIFIER, Accessor.class);
        c1.removeAccessor(admin);
        List<Accessor> accessors = Lists.newArrayList(c1.getAccessors());
        assertEquals(1L, accessors.size());
        assertTrue(accessors.contains(validUser));
    }

    @Test
    public void testGetPermissionScope() throws Exception {
        AccessibleEntity c1 = manager.getFrame("c1", AccessibleEntity.class);
        PermissionScope r1 = manager.getFrame("r1", PermissionScope.class);
        assertEquals(r1, c1.getPermissionScope());
    }

    @Test
    public void testSetPermissionScope() throws Exception {
        AccessibleEntity c1 = manager.getFrame("c1", AccessibleEntity.class);
        PermissionScope r1 = manager.getFrame("r1", PermissionScope.class);
        PermissionScope r2 = manager.getFrame("r2", PermissionScope.class);
        assertEquals(r1, c1.getPermissionScope());
        c1.setPermissionScope(r2);
        assertEquals(r2, c1.getPermissionScope());
    }

    @Test
    public void testGetPermissionScopes() throws Exception {
        AccessibleEntity c1 = manager.getFrame("c1", AccessibleEntity.class);
        AccessibleEntity c2 = manager.getFrame("c2", AccessibleEntity.class);
        PermissionScope r1 = manager.getFrame("r1", PermissionScope.class);
        PermissionScope nl = manager.getFrame("nl", PermissionScope.class);
        assertEquals(r1, c1.getPermissionScope());
        List<PermissionScope> scopes = Lists.newArrayList(c2.getPermissionScopes());
        assertEquals(3L, scopes.size());
        assertTrue(scopes.contains(c1));
        assertTrue(scopes.contains(r1));
        assertTrue(scopes.contains(nl));
    }

    @Test
    public void testGetHistory() throws Exception {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        Mutation<DocumentaryUnit> update = doUpdate(c1);
        assertTrue(update.updated());
        Iterable<SystemEvent> history = c1.getHistory();
        assertEquals(1L, Iterables.size(history));
    }

    @Test
    public void testGetLatestEvent() throws Exception {
        DocumentaryUnit c1 = manager.getFrame("c1", DocumentaryUnit.class);
        assertNull(c1.getLatestEvent());
        doUpdate(c1);
        assertNotNull(c1.getLatestEvent());
    }

    @Test
    public void testHasAccessRestrictions() throws Exception {
        AccessibleEntity ann3 = manager.getFrame("ann3", AccessibleEntity.class);
        // Because ann4 is promoted, access is unrestricted.
        AccessibleEntity ann4 = manager.getFrame("ann4", AccessibleEntity.class);
        assertTrue(ann3.hasAccessRestriction());
        assertFalse(ann4.hasAccessRestriction());
    }

    private Mutation<DocumentaryUnit> doUpdate(DocumentaryUnit unit) throws Exception {
        Bundle doc = new Serializer(graph).vertexFrameToBundle(unit)
                .withDataValue("somekey", "someval");
        Crud<DocumentaryUnit> crud = ViewFactory.getCrudWithLogging(graph, DocumentaryUnit.class);
        return crud.update(doc, validUser);
    }
}
