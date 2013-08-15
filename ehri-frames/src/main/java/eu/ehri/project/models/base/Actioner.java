package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.events.SystemEvent;
import eu.ehri.project.models.utils.JavaHandlerUtils;

public interface Actioner extends NamedEntity {
    /**
     * Fetch a list of Actions for this user in newest-first order.
     * 
     * @return
     */
    @JavaHandler
    public Iterable<SystemEvent> getActions();

    @JavaHandler
    public Iterable<SystemEvent> getLatestAction();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Actioner {
        public Iterable<SystemEvent> getLatestAction() {
            return frameVertices(gremlin()
                    .out(Ontology.ACTIONER_HAS_LIFECYCLE_ACTION)
                    .out(Ontology.ENTITY_HAS_EVENT));
        }

        public Iterable<SystemEvent> getActions() {
            return frameVertices(gremlin().as("n").out(Ontology.ACTIONER_HAS_LIFECYCLE_ACTION)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc)
                    .out(Ontology.ENTITY_HAS_EVENT));
        }
    }
}
