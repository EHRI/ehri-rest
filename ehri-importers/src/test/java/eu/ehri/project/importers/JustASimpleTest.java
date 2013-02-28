/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.importers.test.AbstractImporterTest;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.ActionManager;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author linda
 */
public class JustASimpleTest extends AbstractImporterTest {

    @Test
    public void simpleTest() {
        int userActions = toList(validUser.getActions()).size();
        System.out.println("size : " + userActions);

        int nodeCount = getNodeCount(graph);
        ActionManager am = new ActionManager(graph);
        System.out.println("size : " + toList(validUser.getActions()).size());
        ActionManager.EventContext ctx = am.logEvent(validUser, "Doing something to lots of nodes");
        assertEquals(nodeCount + 2, getNodeCount(graph));

        assertEquals(validUser, ctx.getActioner());
        assertEquals(userActions + 1, toList(validUser.getActions()).size());
        System.out.println("number of events on event: " + toList(ctx.getSystemEvent().getHistory()).size());
        System.out.println("size : " + toList(validUser.getActions()).size());

    }
}
