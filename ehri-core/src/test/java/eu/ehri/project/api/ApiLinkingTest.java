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
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.*;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;


public class ApiLinkingTest extends AbstractFixtureTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testLinks() throws Exception {
        Link link1 = manager.getEntity("link1", Link.class);
        List<Linkable> targets = Lists.newArrayList(link1.getLinkTargets());
        assertTrue(targets.contains(manager.getEntity("c1", Linkable.class)));
        assertTrue(targets.contains(manager.getEntity("c4", Linkable.class)));
    }

    @Test
    public void testGetLinks() throws Exception {
        List<Link> links = Lists.newArrayList(api(adminUser).getLinks("c1"));
        assertEquals(2, links.size());
        assertTrue(links.contains(manager.getEntity("link1", Link.class)));
        assertTrue(links.contains(manager.getEntity("link2", Link.class)));
    }

    @Test
    public void testGetLinksWithAclFilter() throws Exception {
        // Link1 should not be retrieved because target c1 is not visible to invalidUser
        List<Link> links = Lists.newArrayList(api(basicUser).getLinks("c4"));
        assertEquals(2, links.size());
        assertTrue(links.contains(manager.getEntity("link4", Link.class)));
        assertTrue(links.contains(manager.getEntity("link5", Link.class)));
    }

    @Test
    public void testCreateLink() throws Exception {
        DocumentaryUnit src = manager.getEntity("c1", DocumentaryUnit.class);
        HistoricalAgent dst = manager.getEntity("a1", HistoricalAgent.class);
        AccessPoint rel = manager.getEntity("ur1", AccessPoint.class);
        AclManager acl = new AclManager(graph);
        String linkDesc = "Test Link";
        String linkType = "associative";
        Bundle linkBundle = getLinkBundle(linkDesc, linkType);
        // We have to first grant permission to create links...
        acl.grantPermission(src, PermissionType.ANNOTATE, basicUser);
        Link link = loggingApi(basicUser)
                .createLink("c1", "a1", Lists.newArrayList("ur1"),
                        linkBundle, false, Lists.newArrayList(), Optional.empty());
        assertEquals(linkDesc, link.getDescription());
        assertTrue(acl.hasPermission(link, PermissionType.OWNER, basicUser));
        assertEquals(2, Iterables.size(link.getLinkTargets()));
        assertTrue(Iterables.contains(link.getLinkTargets(), src));
        assertTrue(Iterables.contains(link.getLinkTargets(), dst));
        assertNull(link.getLinkSource());
        assertEquals(1, Iterables.size(link.getLinkBodies()));
        assertTrue(Iterables.contains(link.getLinkBodies(), rel));
        assertTrue(Lists.newArrayList(api(basicUser).actionManager()
                .getLatestGlobalEvent().getSubjects()).contains(link));
    }

    @Test
    public void testCreateDirectionalLink() throws Exception {
        DocumentaryUnit src = manager.getEntity("c1", DocumentaryUnit.class);
        HistoricalAgent dst = manager.getEntity("a1", HistoricalAgent.class);
        Link link = loggingApi(adminUser)
                .createLink("c1", "a1", Lists.newArrayList("ur1"),
                        getLinkBundle("test", "associative"),
                        true, Lists.newArrayList(), Optional.empty());
        assertEquals(2, Iterables.size(link.getLinkTargets()));
        assertEquals(src, link.getLinkSource());
    }


    @Test(expected = PermissionDenied.class)
    public void testCreateLinkWithoutPermission() throws Exception {
        api(basicUser).createLink("c1", "a1", Lists.newArrayList("ur1"),
                getLinkBundle("won't work!", "too bad!"), false,
                Lists.newArrayList(), Optional.empty());
    }

    @Test
    public void testCreateAccessPointLink() throws Exception {
        DocumentaryUnit src = manager.getEntity("c1", DocumentaryUnit.class);
        HistoricalAgent dst = manager.getEntity("a1", HistoricalAgent.class);
        DocumentaryUnitDescription desc = manager.getEntity("cd1", DocumentaryUnitDescription.class);
        AclManager acl = new AclManager(graph);
        String linkDesc = "Test Link";
        String linkType = "associative";
        Bundle linkBundle = getLinkBundle(linkDesc, linkType);
        Link link = loggingApi(adminUser)
                .createAccessPointLink("c1", "a1", "cd1",
                        linkDesc, AccessPointType.subject, linkBundle,
                        Lists.newArrayList(adminUser, basicUser), Optional.empty());
        assertNull(linkDesc, link.getLinkField());
        assertTrue(acl.hasPermission(link, PermissionType.OWNER, adminUser));
        assertEquals(linkDesc, link.getDescription());
        assertEquals(2, Iterables.size(link.getLinkTargets()));
        assertTrue(Iterables.contains(link.getLinkTargets(), src));
        assertTrue(Iterables.contains(link.getLinkTargets(), dst));
        assertEquals(1, Iterables.size(link.getLinkBodies()));
        AccessPoint rel = link.getLinkBodies().iterator().next().as(AccessPoint.class);
        assertEquals(rel.getName(), linkDesc);
        assertEquals(rel.getRelationshipType(), AccessPointType.subject);
        Description d = rel.getDescription();
        assertEquals(desc, d);
        assertTrue(link.hasAccessRestriction());
        assertTrue(Lists.newArrayList(api(adminUser).actionManager()
                .getLatestGlobalEvent().getSubjects()).contains(link));
    }

    @Test
    public void testDeleteLink() throws Exception {
        DocumentaryUnit src = manager.getEntity("c1", DocumentaryUnit.class);
        AclManager acl = new AclManager(graph);
        Bundle linkBundle = getLinkBundle("Delete test", "associative");
        // We have to first grant permission to create links...
        acl.grantPermission(src, PermissionType.ANNOTATE, basicUser);
        Link link = loggingApi(basicUser)
                .createLink("c1", "a1", Lists.newArrayList("ur1"),
                        linkBundle, false, Lists.newArrayList(), Optional.empty());
        assertEquals(2, Iterables.size(link.getLinkTargets()));

        assertFalse(acl.hasPermission(ContentTypes.LINK, PermissionType.DELETE, basicUser));
        loggingApi(basicUser).delete(link.getId());
        assertEquals(api(basicUser).actionManager()
                .getLatestGlobalEvent().getEventType(), EventTypes.deletion);
    }

    private Bundle getLinkBundle(String linkDesc, String linkType) {
        return Bundle.Builder.withClass(EntityClass.LINK)
                .addDataValue(Ontology.LINK_HAS_TYPE, linkType)
                .addDataValue(Ontology.LINK_HAS_DESCRIPTION, linkDesc)
                .build();
    }
}