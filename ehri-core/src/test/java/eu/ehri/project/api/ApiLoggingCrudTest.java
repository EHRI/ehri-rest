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

import com.google.common.collect.Lists;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Repository;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.test.AbstractFixtureTest;
import eu.ehri.project.test.TestData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;


public class ApiLoggingCrudTest extends AbstractFixtureTest {

    private ActionManager am;
    private Serializer depSerializer;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        am = new ActionManager(graph);
        depSerializer = new Serializer.Builder(graph).dependentOnly().build();
    }

    @Test
    public void testCreate() throws Exception {
        Bundle repoBundle = Bundle.fromData(TestData.getTestAgentBundle());
        Repository repository = loggingApi(validUser).create(repoBundle, Repository.class);
        assertEquals(repository, am.getLatestGlobalEvent()
                .getSubjects().iterator().next());
    }

    @Test
    public void testCreateOrUpdate() throws Exception {
        Bundle before = depSerializer.entityToBundle(manager.getEntity("r1", Repository.class));
        Bundle repoBundle = Bundle.fromData(TestData.getTestAgentBundle())
                .withId("r1");
        Mutation<Repository> cou = loggingApi(validUser).createOrUpdate(repoBundle, Repository.class);
        assertTrue(cou.updated());
        SystemEvent event = am.getLatestGlobalEvent();
        assertEquals(cou.getNode(), event.getSubjects().iterator().next());
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertNotSame(old, repoBundle);
        Assert.assertEquals(before, old);
    }

    @Test
    public void testUpdate() throws Exception {
        Bundle before = depSerializer.entityToBundle(manager.getEntity("r1", Repository.class));
        Mutation<Repository> cou = loggingApi(validUser)
                .update(before.withDataValue("identifier", "new-id"), Repository.class);
        assertTrue(cou.updated());
        SystemEvent event = am.getLatestGlobalEvent();
        assertEquals(cou.getNode(), event.getSubjects().iterator().next());
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(before, old);
    }

    @Test
    public void testDelete() throws Exception {
        Repository r1 = manager.getEntity("r1", Repository.class);
        Bundle before = depSerializer.entityToBundle(r1);
        loggingApi(validUser).delete("r1");
        SystemEvent event = am.getLatestGlobalEvent();
        assertFalse(manager.exists("r1"));
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(before, old);
        Optional<Version> r1v = loggingApi(validUser).versionManager().versionAtDeletion("r1");
        assertTrue(r1v.isPresent());
        List<Version> r1vl = Lists.newArrayList(loggingApi(validUser).versionManager()
                .versionsAtDeletion(EntityClass.REPOSITORY, null, null));
        assertEquals(1, r1vl.size());
    }
}
