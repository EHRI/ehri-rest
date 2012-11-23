package eu.ehri.project.models.idgen;

import java.util.NoSuchElementException;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.models.Agent;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Generate an ID for the DocumentaryUnit type, which is scoped by its
 * repository.
 * 
 * @author michaelb
 * 
 */
public class DocumentaryUnitIdGenerator implements IdGenerator {

    public String generateId(String entityTypePrefix, Vertex vertex) {
        try {
            Vertex holder = vertex.getVertices(Direction.IN, Agent.HOLDS)
                    .iterator().next();
            return entityTypePrefix + SEPERATOR
                    + holder.getProperty(AccessibleEntity.IDENTIFIER_KEY)
                    + SEPERATOR
                    + vertex.getProperty(AccessibleEntity.IDENTIFIER_KEY);
        } catch (NoSuchElementException e) {
            throw new RuntimeException("No DocumentaryUnit holder found.", e);
        }
    }

}
