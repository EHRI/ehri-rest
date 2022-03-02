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

package eu.ehri.project.acl;

import eu.ehri.project.definitions.Entities;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.ContentType;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class PermissionUtilsTest extends AbstractFixtureTest {

    private PermissionUtils viewHelper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        viewHelper = new PermissionUtils(graph);
    }

    @Test(expected = PermissionDenied.class)
    public void testCheckContentPermissionThrows() throws Exception {
        viewHelper.checkContentPermission(invalidUser,
                ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE);
    }

    @Test
    public void testCheckContentPermissionWithScope() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        viewHelper.setScope(r1).checkContentPermission(invalidUser,
                ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE);
    }

    @Test
    public void testCheckContentPermission() throws Exception {
        // Linda has a global doc unit write grant
        UserProfile user = manager.getEntity("linda", UserProfile.class);
        viewHelper.checkContentPermission(user,
                ContentTypes.DOCUMENTARY_UNIT, PermissionType.CREATE);
    }

    @Test
    public void testCheckEntityPermission() throws Exception {
        UserProfile user = manager.getEntity("reto", UserProfile.class);
        Repository r2 = manager.getEntity("r2", Repository.class);
        viewHelper.checkEntityPermission(r2, user, PermissionType.UPDATE);
    }

    @Test(expected = PermissionDenied.class)
    public void testCheckEntityPermissionThrows() throws Exception {
        UserProfile user = manager.getEntity("linda", UserProfile.class);
        Repository r2 = manager.getEntity("r2", Repository.class);
        viewHelper.checkEntityPermission(r2, user, PermissionType.UPDATE);
    }

    @Test
    public void testCheckReadAccess() throws Exception {
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        viewHelper.checkReadAccess(c1, validUser);
    }

    @Test(expected = AccessDenied.class)
    public void testCheckReadAccessThrows() throws Exception {
        DocumentaryUnit c1 = manager.getEntity("c1", DocumentaryUnit.class);
        viewHelper.checkReadAccess(c1, invalidUser);
    }

    @Test
    public void testGetContentTypeNodeFromEntityType() throws Exception {
        assertEquals(manager.getEntity(Entities.REPOSITORY, ContentType.class),
                viewHelper.getContentTypeNode(EntityClass.REPOSITORY));
    }

    @Test
    public void testGetContentTypeFromClass() throws Exception {
        assertEquals(ContentTypes.REPOSITORY,
                viewHelper.getContentTypeEnum(Repository.class));
    }
}
