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

package eu.ehri.project.api;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.DocumentaryUnitDescription;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.HistoricalAgent;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


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
    public void testCreateLink() throws Exception {
        DocumentaryUnit src = manager.getEntity("c1", DocumentaryUnit.class);
        HistoricalAgent dst = manager.getEntity("a1", HistoricalAgent.class);
        AccessPoint rel = manager.getEntity("ur1", AccessPoint.class);
        String linkDesc = "Test Link";
        String linkType = "associative";
        Bundle linkBundle = getLinkBundle(linkDesc, linkType);
        Link link = api(validUser).createLink("c1", "a1", Lists.newArrayList("ur1"),
                linkBundle, Lists.<Accessor>newArrayList());
        assertEquals(linkDesc, link.getDescription());
        assertEquals(2, Iterables.size(link.getLinkTargets()));
        assertTrue(Iterables.contains(link.getLinkTargets(), src));
        assertTrue(Iterables.contains(link.getLinkTargets(), dst));
        assertEquals(1, Iterables.size(link.getLinkBodies()));
        assertTrue(Iterables.contains(link.getLinkBodies(), rel));
        assertTrue(Lists.newArrayList(api(validUser).actionManager()
                .getLatestGlobalEvent().getSubjects()).contains(link));
    }


    @Test(expected = PermissionDenied.class)
    public void testCreateLinkWithoutPermission() throws Exception {
        api(invalidUser).createLink("c1", "a1", Lists.newArrayList("ur1"),
                getLinkBundle("won't work!", "too bad!"),
                Lists.<Accessor>newArrayList());
    }

    @Test
    public void testCreateAccessPointLink() throws Exception {
        DocumentaryUnit src = manager.getEntity("c1", DocumentaryUnit.class);
        HistoricalAgent dst = manager.getEntity("a1", HistoricalAgent.class);
        DocumentaryUnitDescription desc = manager.getEntity("cd1", DocumentaryUnitDescription.class);
        String linkDesc = "Test Link";
        String linkType = "associative";
        Bundle linkBundle = getLinkBundle(linkDesc, linkType);
        Link link = api(validUser).createAccessPointLink("c1", "a1", "cd1",
                linkDesc, AccessPointType.subject, linkBundle,
                Lists.<Accessor>newArrayList(validUser, invalidUser));
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
        assertTrue(Lists.newArrayList(api(validUser).actionManager()
                .getLatestGlobalEvent().getSubjects()).contains(link));
    }

    private Bundle getLinkBundle(String linkDesc, String linkType) {
        return Bundle.Builder.withClass(EntityClass.LINK)
                .addDataValue(Ontology.LINK_HAS_TYPE, linkType)
                .addDataValue(Ontology.LINK_HAS_DESCRIPTION, linkDesc)
                .build();
    }
}