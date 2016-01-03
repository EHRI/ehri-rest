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

package eu.ehri.project.acl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.Country;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.GraphTestBase;
import eu.ehri.project.test.TestData;
import eu.ehri.project.utils.GraphInitializer;
import eu.ehri.project.utils.fixtures.FixtureLoader;
import eu.ehri.project.utils.fixtures.FixtureLoaderFactory;
import eu.ehri.project.views.impl.LoggingCrudViews;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static eu.ehri.project.acl.ContentTypes.COUNTRY;
import static eu.ehri.project.acl.ContentTypes.DOCUMENTARY_UNIT;
import static eu.ehri.project.acl.ContentTypes.REPOSITORY;
import static eu.ehri.project.acl.PermissionType.CREATE;
import static eu.ehri.project.acl.PermissionType.DELETE;
import static eu.ehri.project.acl.PermissionType.OWNER;
import static eu.ehri.project.acl.PermissionType.UPDATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: michaelb
 */
public class AclManagerTest extends GraphTestBase {

    private FixtureLoader loader;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        loader = FixtureLoaderFactory.getInstance(graph, false); // Initialize separately
        new GraphInitializer(graph).initialize();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testIsAdmin() throws Exception {
        Group admin = manager.getEntity(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        assertTrue(new AclManager(graph).belongsToAdmin(admin));
    }

    @Test
    public void testBelongsToAdmin() throws Exception {
        loader.loadTestData();
        UserProfile mike = manager.getEntity("mike", UserProfile.class);
        assertTrue(new AclManager(graph).belongsToAdmin(mike));

        // Test a user not directly in admin, but belonging to a group that is,
        // is also an admin user.
        UserProfile tim = manager.getEntity("tim", UserProfile.class);
        assertTrue(new AclManager(graph).belongsToAdmin(tim));
    }

    @Test
    public void testIsAnonymous() throws Exception {
        Accessor admin = manager.getEntity(Group.ADMIN_GROUP_IDENTIFIER, Group.class);
        Accessor anon = AnonymousAccessor.getInstance();
        AclManager acl = new AclManager(graph);
        assertFalse(acl.belongsToAdmin(anon));
        assertTrue(acl.isAnonymous(anon));
        assertFalse(acl.isAnonymous(admin));
    }

    @Test
    public void testGetAccessControl() throws Exception {
        loader.loadTestData();
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        UserProfile user1 = manager.getEntity("mike", UserProfile.class);
        UserProfile user2 = manager.getEntity("reto", UserProfile.class);
        AclManager acl = new AclManager(graph);
        assertTrue(acl.canAccess(c1, user1));
        assertFalse(acl.canAccess(c1, user2));
    }

    @Test
    public void testRemoveAccessControl() throws Exception {
        loader.loadTestData();
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        UserProfile user1 = manager.getEntity("mike", UserProfile.class);
        AclManager acl = new AclManager(graph);
        assertTrue(Iterables.contains(c1.getAccessors(), user1));
        acl.removeAccessControl(c1, user1);
        assertFalse(Iterables.contains(c1.getAccessors(), user1));
    }

    @Test
    public void testSetAccessors() throws Exception {
        loader.loadTestData();
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        UserProfile user1 = manager.getEntity("mike", UserProfile.class);
        UserProfile user2 = manager.getEntity("reto", UserProfile.class);
        AclManager acl = new AclManager(graph);
        assertTrue(acl.canAccess(c1, user1));
        assertFalse(acl.canAccess(c1, user2));
        acl.setAccessors(c1, Lists.<Accessor>newArrayList(user1, user2));
        assertTrue(acl.canAccess(c1, user1));
        assertTrue(acl.canAccess(c1, user2));
    }

    @Test
    public void testGetInheritedEntityPermissions() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        DocumentaryUnit c4 = manager.getEntity("c4", DocumentaryUnit.class);
        UserProfile user = manager.getEntity("reto", UserProfile.class);

        // ? Because we can create docs within this scope (r1), we
        // can also create CHILD docs of docs within that scope.
        // Should this behaviour be allowed? For the moment it
        // is...
        InheritedItemPermissionSet permissions
                = acl.getInheritedItemPermissions(c4, user);
        assertTrue(permissions.has(CREATE));
        assertFalse(permissions.has(UPDATE));
        assertFalse(permissions.has(DELETE));
        assertFalse(permissions.has(OWNER));
    }

    @Test
    public void testSetEntityPermissions() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        DocumentaryUnit c4 = manager.getEntity("c4", DocumentaryUnit.class);
        UserProfile user = manager.getEntity("reto", UserProfile.class);

        // ? Because we can create docs within this scope (r1), we
        // can also create CHILD docs of docs within that scope.
        // Should this behaviour be allowed? For the moment it
        // is...
        InheritedItemPermissionSet permissions
                = acl.getInheritedItemPermissions(c4, user);
        assertTrue(permissions.has(CREATE));
        assertFalse(permissions.has(UPDATE));
        assertFalse(permissions.has(DELETE));
        assertFalse(permissions.has(OWNER));

        acl.setItemPermissions(c4, user, Sets.newHashSet(DELETE, UPDATE));
        InheritedItemPermissionSet permissions2 = acl.getInheritedItemPermissions(c4, user);
        assertTrue(permissions2.has(CREATE));
        assertTrue(permissions2.has(UPDATE));
        assertTrue(permissions2.has(DELETE));
        assertFalse(permissions2.has(OWNER));
    }

    @Test
    public void testSetPermissionMatrix() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        UserProfile user = manager.getEntity("reto", UserProfile.class);

        GlobalPermissionSet permissions = acl.getGlobalPermissions(user);
        assertFalse(permissions.has(COUNTRY, CREATE));
        acl.setPermissionMatrix(user, permissions.withPermission(COUNTRY, CREATE));
        assertTrue(acl.getGlobalPermissions(user).has(COUNTRY, CREATE));
    }

    @Test
    public void testGrantPermission() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        UserProfile user = manager.getEntity("reto", UserProfile.class);
        DocumentaryUnit c4 = manager.getEntity("c4", DocumentaryUnit.class);

        assertFalse(acl.hasPermission(c4, OWNER, user));
        PermissionGrant grant = acl.grantPermission(c4, OWNER, user);
        assertTrue(acl.hasPermission(c4, OWNER, user));

        // Try granting the same permission twice and ensure the
        // returned grant is the same instance as before...
        PermissionGrant grant2 = acl.grantPermission(c4, OWNER, user);
        assertEquals(grant, grant2);
    }

    @Test
    public void testContentTypeFilterFunction() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);

        Vertex c1 = manager.getVertex("c1");
        Vertex r1 = manager.getVertex("r1");
        // cd1 is not a content type
        Vertex cd1 = manager.getVertex("cd1");
        List<Vertex> frames = Lists.newArrayList(c1, r1, cd1);

        GremlinPipeline<Vertex, Vertex> pipeline = new GremlinPipeline<Vertex, Vertex>(frames);
        List<Vertex> filtered = pipeline
                .filter(acl.getContentTypeFilterFunction()).toList();
        assertEquals(2L, filtered.size());
        assertFalse(filtered.contains(cd1));
    }

    @Test
    public void testGetAclFilterFunction() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);

        UserProfile user = manager.getEntity("reto", UserProfile.class);

        Annotation ann3v = manager.getEntity("ann3", Annotation.class); // hidden from user
        Annotation ann4v = manager.getEntity("ann4", Annotation.class); // promoted, thus visible
        assertFalse(acl.canAccess(ann3v, user));
        assertTrue(acl.canAccess(ann4v, user));

        PipeFunction<Vertex,Boolean> aclFilter = acl.getAclFilterFunction(user);
        List<Vertex> filtered = new GremlinPipeline<Vertex,Vertex>(
                Lists.newArrayList(ann3v.asVertex(), ann4v.asVertex())).filter(aclFilter).toList();
        assertEquals(1L, filtered.size());
        assertFalse(filtered.contains(ann3v.asVertex()));
    }

    @Test
    public void testRevokePermission() throws Exception {

        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        UserProfile user = manager.getEntity("reto", UserProfile.class);
        DocumentaryUnit c4 = manager.getEntity("c4", DocumentaryUnit.class);

        assertFalse(acl.hasPermission(c4, OWNER, user));
        acl.grantPermission(c4, OWNER, user);
        assertTrue(acl.hasPermission(c4, OWNER, user));
        acl.revokePermission(c4, OWNER, user);
        assertFalse(acl.hasPermission(c4, OWNER, user));
    }

    @Test
    public void testRevokePermissionGrant() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        UserProfile user = manager.getEntity("reto", UserProfile.class);
        Repository r1 = manager.getEntity("r1", Repository.class);
        PermissionGrant grant
                = manager.getEntity("retoKclWriteGrant", PermissionGrant.class);
        assertFalse(acl.hasPermission(DOCUMENTARY_UNIT, CREATE, user));
        assertTrue(acl.withScope(r1).hasPermission(DOCUMENTARY_UNIT, CREATE, user));
        acl.revokePermissionGrant(grant);
        assertFalse(acl.withScope(r1).hasPermission(DOCUMENTARY_UNIT, CREATE, user));
    }

    @Test
    public void testGetAdminPermissions() throws Exception {
        loader.loadTestData();
        AclManager acl = new AclManager(graph);
        Accessor admin = manager.getEntity(Group.ADMIN_GROUP_IDENTIFIER, Accessor.class);
        GlobalPermissionSet permissionSet = acl.getGlobalPermissions(admin);
        // Admin should have ALL the permissions!
        for (ContentTypes contentType : ContentTypes.values()) {
            for (PermissionType permissionType : PermissionType.values()) {
                assertTrue(permissionSet.has(contentType, permissionType));
            }
        }
    }

    @Test
    public void testGetInheritedGlobalPermissions() throws Exception {
        loader.loadTestData("permissions.yaml");
        Group group1 = manager.getEntity("group1", Group.class);
        UserProfile user1 = manager.getEntity("user1", UserProfile.class);

        AclManager acl = new AclManager(graph);
        List<Map<String, GlobalPermissionSet>> getPerms
                = acl.getInheritedGlobalPermissions(user1).serialize();
        // It should contain two elements - the user, and his group
        assertEquals(2, getPerms.size());
        assertEquals(GlobalPermissionSet.empty(), getPerms.get(0).get(user1.getId()));
        GlobalPermissionSet groupPerms = acl.getGlobalPermissions(group1);
        assertEquals(groupPerms, getPerms.get(1).get(group1.getId()));
    }

    @Test
    public void testGetGlobalPermissions() throws Exception {
        loader.loadTestData("permissions.yaml");
        Group group1 = manager.getEntity("group1", Group.class);
        UserProfile user1 = manager.getEntity("user1", UserProfile.class);

        // Group1 should have create/update/delete for documentaryUnits
        GlobalPermissionSet hasPerms = GlobalPermissionSet.newBuilder()
            .set(DOCUMENTARY_UNIT, CREATE, UPDATE, DELETE).build();
        GlobalPermissionSet getPerms = new AclManager(graph).getGlobalPermissions(group1);

        // Bit of a hassle comparing the data structures..
        assertEquals(hasPerms, getPerms);

        // User1 should have an empty set of global permissions
        assertEquals(GlobalPermissionSet.empty(), new AclManager(graph)
                .getGlobalPermissions(user1));
    }

    @Test
    public void testCountryScopeScenario() throws Exception {
        loader.loadTestData("country-permissions.yaml");

        Country gb = manager.getEntity("gb", Country.class);
        Country nl = manager.getEntity("nl", Country.class);
        UserProfile gbuser = manager.getEntity("gbuser", UserProfile.class);
        UserProfile nluser = manager.getEntity("nluser", UserProfile.class);

        AclManager acl = new AclManager(graph);

        assertTrue(acl.withScope(gb).hasPermission(REPOSITORY, CREATE, gbuser));
        assertFalse(acl.withScope(nl).hasPermission(REPOSITORY, CREATE, gbuser));

        // Create a repository
        // New create some stuff
        LoggingCrudViews<Repository> repoViews
                = new LoggingCrudViews<Repository>(graph, Repository.class);

        Repository gbrepo = repoViews.setScope(gb).create(
                Bundle.fromData(TestData.getTestAgentBundle()), gbuser);
        Repository nlrepo = repoViews.setScope(nl).create(
                Bundle.fromData(TestData.getTestAgentBundle()), nluser);

        assertTrue(acl.withScope(gbrepo).hasPermission(DOCUMENTARY_UNIT, CREATE, gbuser));
        assertTrue(acl.withScope(gbrepo).hasPermission(DOCUMENTARY_UNIT, UPDATE, gbuser));
        assertTrue(acl.withScope(gbrepo).hasPermission(DOCUMENTARY_UNIT, DELETE, gbuser));
        assertFalse(acl.withScope(nlrepo).hasPermission(DOCUMENTARY_UNIT, CREATE, gbuser));
        assertFalse(acl.withScope(nlrepo).hasPermission(DOCUMENTARY_UNIT, UPDATE, gbuser));
        assertFalse(acl.withScope(nlrepo).hasPermission(DOCUMENTARY_UNIT, DELETE, gbuser));
    }

    @Test
    public void testCreateOwnerScenario() throws Exception {
        loader.loadTestData("archivist-permissions.yaml");
        UserProfile headUser = manager.getEntity("hauser", UserProfile.class);
        UserProfile user1 = manager.getEntity("auser1", UserProfile.class);
        UserProfile user2 = manager.getEntity("auser2", UserProfile.class);
        Group headArchivists = manager.getEntity("head-archivists", Group.class);
        Group archivists = manager.getEntity("archivists", Group.class);
        Repository repo = manager.getEntity("repo", Repository.class);

        AclManager acl = new AclManager(graph, repo);
        // The grants in the permissions should give these particular
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, CREATE, headArchivists));
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, UPDATE, headArchivists));
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, DELETE, headArchivists));

        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, CREATE, archivists));
        assertFalse(acl.hasPermission(DOCUMENTARY_UNIT, UPDATE, archivists));
        assertFalse(acl.hasPermission(DOCUMENTARY_UNIT, DELETE, archivists));

        // Check these grants are correctly inherited by the users.
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, CREATE, headUser));
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, UPDATE, headUser));
        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, DELETE, headUser));

        assertTrue(acl.hasPermission(DOCUMENTARY_UNIT, CREATE, user1));
        assertFalse(acl.hasPermission(DOCUMENTARY_UNIT, UPDATE, user1));
        assertFalse(acl.hasPermission(DOCUMENTARY_UNIT, DELETE, user1));

        // New create some stuff
        LoggingCrudViews<DocumentaryUnit> views
                = new LoggingCrudViews<DocumentaryUnit>(graph, DocumentaryUnit.class, repo);

        DocumentaryUnit headdoc1 = views.create(
                Bundle.fromData(TestData.getTestDocBundle())
                        .withDataValue(Ontology.IDENTIFIER_KEY, "head-doc"), headUser);
        DocumentaryUnit userdoc1 = views.create(
                Bundle.fromData(TestData.getTestDocBundle())
                        .withDataValue(Ontology.IDENTIFIER_KEY, "user-doc-1"), user1);
        DocumentaryUnit userdoc2 = views.create(
                Bundle.fromData(TestData.getTestDocBundle())
                        .withDataValue(Ontology.IDENTIFIER_KEY, "user-doc-2"), user2);

        // Ensure Head Archivist can update/delete user1's doc
        assertTrue(acl.hasPermission(userdoc1, UPDATE, headArchivists));
        assertTrue(acl.hasPermission(userdoc1, DELETE, headArchivists));
        // But doesn't own it
        assertFalse(acl.hasPermission(userdoc1, OWNER, headArchivists));

        // Check the calculated permission sets.
        assertTrue(acl.getInheritedItemPermissions(userdoc1, user1).has(OWNER));
        assertFalse(acl.getInheritedItemPermissions(userdoc2, user1).has(OWNER));
        assertTrue(acl.getInheritedItemPermissions(userdoc2, user2).has(OWNER));
        assertFalse(acl.getInheritedItemPermissions(userdoc1, user2).has(OWNER));

        // Ensure user1 can update/delete his own doc
        assertTrue(acl.hasPermission(userdoc1, OWNER, user1));
        assertTrue(acl.hasPermission(userdoc1, UPDATE, user1));
        assertTrue(acl.hasPermission(userdoc1, DELETE, user1));

        // Ensure neither user1 or user2 can update/delete head's docs
        assertFalse(acl.hasPermission(headdoc1, OWNER, user1));
        assertFalse(acl.hasPermission(headdoc1, UPDATE, user1));
        assertFalse(acl.hasPermission(headdoc1, DELETE, user1));
        assertFalse(acl.hasPermission(headdoc1, OWNER, user2));
        assertFalse(acl.hasPermission(headdoc1, UPDATE, user2));
        assertFalse(acl.hasPermission(headdoc1, DELETE, user2));

        // Ensure neither user1 or user2 can update/delete each other's docs
        assertFalse(acl.hasPermission(userdoc1, OWNER, user2));
        assertFalse(acl.hasPermission(userdoc1, UPDATE, user2));
        assertFalse(acl.hasPermission(userdoc1, DELETE, user2));
        assertFalse(acl.hasPermission(userdoc2, OWNER, user1));
        assertFalse(acl.hasPermission(userdoc2, UPDATE, user1));
        assertFalse(acl.hasPermission(userdoc2, DELETE, user1));
    }
}
