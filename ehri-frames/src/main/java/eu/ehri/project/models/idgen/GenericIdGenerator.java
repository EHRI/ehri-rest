package eu.ehri.project.models.idgen;

import com.tinkerpop.blueprints.Vertex;

import java.util.UUID;

/**
 * Generates a generic ID for tertiary node types.
 * 
 * @author michaelb
 * 
 */
public class GenericIdGenerator implements IdGenerator {

    /**
     * Generates a random string UUID.
     */
    public String generateId(String entityTypePrefix, Vertex vertex) {
        return entityTypePrefix + SEPERATOR + UUID.randomUUID();
    }
}
