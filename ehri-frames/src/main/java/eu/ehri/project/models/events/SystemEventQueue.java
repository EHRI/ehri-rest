package eu.ehri.project.models.events;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Class representing the system event queue node, of which
 * there Will Be Only One.
 *
 * Perhaps we should enforce that somehow???
 */
@EntityType(EntityClass.SYSTEM)
public interface SystemEventQueue extends VertexFrame {

    @GremlinGroovy("_().out('lifecycleActionStream')._()"
            + ".copySplit(_(), _().as('n').out('lifecycleAction')"
            + ".loop('n'){true}{true}).exhaustMerge")
    public Iterable<SystemEvent> getSystemEvents();
}
