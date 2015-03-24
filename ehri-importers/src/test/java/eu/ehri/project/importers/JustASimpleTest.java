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

package eu.ehri.project.importers;

import com.google.common.base.Optional;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.persistence.ActionManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class JustASimpleTest extends AbstractImporterTest {
private static final Logger logger = LoggerFactory.getLogger(JustASimpleTest.class);
    @Test
    public void simpleTest() {
        int userActions = toList(validUser.getActions()).size();
        logger.info("size : " + userActions);

        int nodeCount = getNodeCount(graph);
        ActionManager am = new ActionManager(graph);
        logger.info("size : " + toList(validUser.getActions()).size());
        ActionManager.EventContext ctx = am.logEvent(validUser,
                EventTypes.creation,
                Optional.of("Doing something to lots of nodes"));
        assertEquals(nodeCount + 2, getNodeCount(graph));

        assertEquals(validUser, ctx.getActioner());
        assertEquals(userActions + 1, toList(validUser.getActions()).size());
        logger.info("number of events on event: " + toList(ctx.getSystemEvent().getHistory()).size());
        logger.info("size : " + toList(validUser.getActions()).size());

    }
}
