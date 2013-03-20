package eu.ehri.project.models.base;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.persistance.ActionManager;

public interface Actioner extends VertexFrame {

    @Property("name")
    public String getName();

    /**
     * Fetch a list of Actions for this user in newest-first order.
     * 
     * @return
     */
    @GremlinGroovy("_().as('n').out('" + ActionManager.LIFECYCLE_ACTION + "')" +
            ".loop('n'){true}{true}.out('" + SystemEvent.HAS_EVENT + "')")
    public Iterable<SystemEvent> getActions();

    @GremlinGroovy("_().as('n').out('" + ActionManager.LIFECYCLE_ACTION + "')"
            + ".out('" + SystemEvent.HAS_EVENT + "')")
    public Iterable<SystemEvent> getLatestAction();
}
