package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

import eu.ehri.project.models.events.Action;
import eu.ehri.project.models.events.GlobalEvent;
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
            ".loop('n'){true}{true}.out('" + GlobalEvent.HAS_EVENT + "')")
    public Iterable<GlobalEvent> getActions();

    @GremlinGroovy("_().as('n').out('" + ActionManager.LIFECYCLE_ACTION + "')"
            + ".out('" + GlobalEvent.HAS_EVENT + "')")
    public Iterable<GlobalEvent> getLatestAction();
}
