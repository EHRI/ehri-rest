package eu.ehri.project.models.events;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerImpl;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.util.Pipeline;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.utils.JavaHandlerUtils;
import eu.ehri.project.persistance.ActionManager;

/**
 * Class representing the system event queue node, of which
 * there Will Be Only One.
 *
 * Perhaps we should enforce that somehow???
 */
@EntityType(EntityClass.SYSTEM)
public interface SystemEventQueue extends Frame {

    public static final String STREAM_START = ActionManager.LIFECYCLE_ACTION + "Stream";

    @JavaHandler
    public Iterable<SystemEvent> getSystemEvents();

    abstract class Impl implements JavaHandlerImpl<Vertex>, SystemEventQueue {
        public Iterable<SystemEvent> getSystemEvents() {
            Pipeline<Vertex,Vertex> otherPipe = gremlin().as("n")
                    .out(ActionManager.LIFECYCLE_ACTION)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc);
            return frameVertices(gremlin()
                    .out(STREAM_START).cast(Vertex.class)
                    .copySplit(gremlin(), otherPipe)
                    .exhaustMerge().cast(Vertex.class));
        }
    }
}
