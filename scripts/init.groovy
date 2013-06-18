// Initialise a groovy shell
//

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.models.*;
import eu.ehri.project.persistence.*;

NEO4J_HOME = System.getenv()['NEO4J_HOME'];
if (NEO4J_HOME == null) {
    throw new IllegalArgumentException("NEO4J_HOME env var not defined.");
}

NEO4J_DB = System.getenv()['NEO4J_DB'];
if (NEO4J_DB == null) {
    NEO4J_DB = new File(NEO4J_HOME, "data", "graph.db").getPath();
}

graph = new FramedGraph(new Neo4jGraph(NEO4J_DB));
manager = GraphManagerFactory.getInstance(graph);



