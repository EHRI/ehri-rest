/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *
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
