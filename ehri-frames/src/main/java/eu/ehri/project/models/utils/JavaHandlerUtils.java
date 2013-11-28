package eu.ehri.project.models.utils;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;

/**
 * User: mike
 *
 * Utilities for dealing with Gremlin pipelines.
 */
public class JavaHandlerUtils {

    public static int LOOP_MAX = 20;

    /**
     * Pipe function that quits after a certain number of loops
     */
    public static <S> PipeFunction<LoopPipe.LoopBundle<S>, Boolean> maxLoopFuncFactory(final int maxLoops) {
        return new PipeFunction<LoopPipe.LoopBundle<S>,
                Boolean>() {
            @Override
            public Boolean compute(LoopPipe.LoopBundle<S> vertexLoopBundle) {
                return vertexLoopBundle.getLoops() < maxLoops;
            }
        };
    }

    /**
     * Pipe function with a max loop clause with the default of 20.
     */
    public static PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean> defaultMaxLoops
            = maxLoopFuncFactory(LOOP_MAX);

    /**
     * Pipe function that always allows looping to continue.
     */
    public static PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean> noopLoopFunc
            = new PipeFunction<com.tinkerpop.pipes.branch.LoopPipe.LoopBundle<Vertex>, Boolean>() {
        @Override
        public Boolean compute(com.tinkerpop.pipes.branch.LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
            return true;
        }
    };

    /**
     * Given a pipeline
     * @param element   The element
     * @param propName  The cache property name
     * @param pipe      A pipeline to count
     */
    public static <S,E> void cacheCount(Element element, GremlinPipeline<S,E> pipe, String propName) {
        element.setProperty(propName, pipe.count());
    }

    public static <S,E> Long getCachedCount(Element element, GremlinPipeline<S,E> pipe, String propName) {
        Long count = element.getProperty(propName);
        if (count == null) {
            count = pipe.count();
        }
        return count;
    }
}
