package eu.ehri.project.utils.fixtures;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.utils.fixtures.impl.YamlFixtureLoader;

/**
 * Factory class for concealing details of Fixture loading
 * implementation.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 *
 */
public class FixtureLoaderFactory {
    /**
     * Get an instance of a fixture loader for the given class.
     * 
     * @param graph
     * @return
     */
    public static FixtureLoader getInstance(FramedGraph<? extends TransactionalGraph> graph) {
        return new YamlFixtureLoader(graph);
    }

    /**
     * Get an instance of a fixture loader for the given class, specifying
     * whether or not to initialize the graph before loading.
     *
     * @param graph
     * @param initialize
     * @return
     */
    public static FixtureLoader getInstance(FramedGraph<? extends TransactionalGraph> graph, boolean initialize) {
        return new YamlFixtureLoader(graph, initialize);
    }
}
