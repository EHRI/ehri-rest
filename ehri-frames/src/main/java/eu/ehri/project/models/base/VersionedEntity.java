package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.events.Version;
import eu.ehri.project.models.utils.JavaHandlerUtils;

public interface VersionedEntity extends Frame {
    @Adjacency(label = Ontology.ENTITY_HAS_PRIOR_VERSION, direction = Direction.OUT)
    public Version getPriorVersion();

    @JavaHandler
    public Iterable<Version> getAllPriorVersions();

    abstract class Impl implements JavaHandlerContext<Vertex>, VersionedEntity {

        /**
         * Loops up through the chain of hasPriorVersion until the end, until it
         * has been deleted, will be a item.
         * @return
         */
        public Iterable<Version> getAllPriorVersions() {
            return frameVertices(gremlin().as("n").out(Ontology.ENTITY_HAS_PRIOR_VERSION)
                    .loop("n", JavaHandlerUtils.noopLoopFunc, JavaHandlerUtils.noopLoopFunc));
        }

    }

}
