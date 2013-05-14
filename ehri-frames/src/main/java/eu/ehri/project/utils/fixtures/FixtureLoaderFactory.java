package eu.ehri.project.utils.fixtures;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.utils.fixtures.impl.YamlFixtureLoader;

/**
 * Factory class for concealing details of Fixture loading
 * implementation.
 * 
 * @author mike
 *
 */
public class FixtureLoaderFactory {
    /**
     * Get an instance of a fixture loader for the given class.
     * 
     * @param graph
     * @return
     */
    public static FixtureLoader getInstance(FramedGraph<Neo4jGraph> graph) {
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
    public static FixtureLoader getInstance(FramedGraph<Neo4jGraph> graph, boolean initialize) {
        return new YamlFixtureLoader(graph, initialize);
    }
}
