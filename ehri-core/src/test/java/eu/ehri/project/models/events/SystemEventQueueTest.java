/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.models.events;

import com.google.common.collect.Iterators;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class SystemEventQueueTest extends AbstractFixtureTest {

    @Test
    public void testGetLatestEvent() throws Exception {
        SystemEventQueue queue = manager.getEntity(ActionManager.GLOBAL_EVENT_ROOT, SystemEventQueue.class);
        assertNull(queue.getLatestEvent());
        ActionManager.EventContext ctx = new ActionManager(graph)
                .newEventContext(adminUser, adminUser.as(Actioner.class), EventTypes.creation);
        SystemEvent commit = ctx.commit();
        assertEquals(commit, queue.getLatestEvent());
    }

    @Test
    public void testGetSystemEvents() throws Exception {
        SystemEventQueue queue = manager.getEntity(ActionManager.GLOBAL_EVENT_ROOT, SystemEventQueue.class);
        new ActionManager(graph)
                .newEventContext(adminUser, adminUser.as(Actioner.class), EventTypes.creation)
                .commit();
        assertEquals(1, Iterators.size(queue.getSystemEvents().iterator()));
    }
}