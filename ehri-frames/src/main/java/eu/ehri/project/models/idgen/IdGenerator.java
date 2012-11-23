package eu.ehri.project.models.idgen;

import com.tinkerpop.blueprints.Vertex;

/**
 * Generate an ID given an entity type and a vertex.
 * @author michaelb
 *
 */
public interface IdGenerator {
    
    /**
     * Separate or ID components.
     */
    public static final String SEPERATOR = "-";
    
    /**
     * Generate an ID given an entity type prefix and a vertex.
     * 
     * @param entityTypePrefix
     * @param vertex
     * @return
     */
    public String generateId(String entityTypePrefix, Vertex vertex);
}
