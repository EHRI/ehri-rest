package eu.ehri.project.models.idgen;

import com.tinkerpop.blueprints.Vertex;

import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Generates an ID for nodes which represent AccessibleEntities.
 * 
 * @author michaelb
 * 
 */
public class AccessibleEntityIdGenerator implements IdGenerator {

    /**
     * Uses the items identifier and its entity type to generate a (supposedly)
     * unique ID.
     */
    public String generateId(String entityTypePrefix, Vertex vertex) {
        return entityTypePrefix + SEPERATOR
                + vertex.getProperty(AccessibleEntity.IDENTIFIER_KEY);
    }

}
