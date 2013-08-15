package eu.ehri.project.models.utils;

import com.tinkerpop.blueprints.Vertex;
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
    public static PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean> maxLoopFuncFactory(final int maxLoops) {
        return new PipeFunction<LoopPipe.LoopBundle<Vertex>,
                Boolean>() {
            @Override
            public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
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
}
