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
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.DocumentaryUnit;
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
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;


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
        Repository repository = loggingApi(adminUser).create(repoBundle, Repository.class);
        assertEquals(repository, am.getLatestGlobalEvent()
                .getSubjects().iterator().next());
    }

    @Test
    public void testUpdate() throws Exception {
        Bundle before = depSerializer.entityToBundle(manager.getEntity("r1", Repository.class));
        Mutation<Repository> cou = loggingApi(adminUser)
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
        Repository r1 = manager.getEntity("r2", Repository.class);
        Bundle before = depSerializer.entityToBundle(r1);
        loggingApi(adminUser).delete("r2");
        SystemEvent event = am.getLatestGlobalEvent();
        assertFalse(manager.exists("r2"));
        assertTrue(event.getPriorVersions().iterator().hasNext());
        Bundle old = Bundle.fromString(event.getPriorVersions().iterator().next().getEntityData());
        assertEquals(before, old);
        Optional<Version> r1v = loggingApi(adminUser).versionManager().versionAtDeletion("r2");
        assertTrue(r1v.isPresent());
        Optional<Version> r1v2 = loggingApi(adminUser).versionManager().versionAtDeletion("r2-1234", true);
        assertTrue(r1v2.isPresent());
        List<Version> r1vl = Lists.newArrayList(loggingApi(adminUser).versionManager()
                .versionsAtDeletion(EntityClass.REPOSITORY, null, null));
        assertEquals(1, r1vl.size());
    }

    @Test
    public void testDeleteWithTimestampOnItemNotFound() throws Exception {
        String id = "c4";
        loggingApi(adminUser).delete(id);
        try {
            loggingApi(adminUser).get(id, DocumentaryUnit.class);
            fail("Should have thrown " + ItemNotFound.class.getSimpleName());
        } catch (ItemNotFound e) {
            assertTrue(e.getDeletedAt().isPresent());
        }
        try {
            loggingApi(adminUser).getByPid(id + "-12345678", DocumentaryUnit.class);
            fail("Should have thrown " + ItemNotFound.class.getSimpleName());
        } catch (ItemNotFound e) {
            assertTrue(e.getDeletedAt().isPresent());
        }
    }

    @Test
    public void testDeleteChildren() throws Exception {
        List<String> out = loggingApi(adminUser).deleteChildren(item.getId(), true, true, Optional.empty());
        assertEquals(Lists.newArrayList("c2", "c3"), out);
        SystemEvent event = am.getLatestGlobalEvent();
        assertEquals(EventTypes.deletion, event.getEventType());
        assertEquals(Iterables.size(event.getPriorVersions()), 2);
        assertEquals(item, event.getEventScope());
    }
}
