package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.Action;

public interface Actioner extends VertexFrame {

    @Property("name")
    public String getName();

    @Adjacency(label = Action.HAS_ACTIONER, direction = Direction.IN)
    public Iterable<Action> getActions();
}
