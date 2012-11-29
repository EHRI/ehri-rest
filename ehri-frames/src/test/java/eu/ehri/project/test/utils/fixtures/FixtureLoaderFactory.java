package eu.ehri.project.test.utils.fixtures;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.test.utils.fixtures.impl.JsonFixtureLoader;

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
    public FixtureLoader getInstance(FramedGraph<Neo4jGraph> graph) {
        return new JsonFixtureLoader(graph);
    }
}
