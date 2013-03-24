package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import eu.ehri.project.models.annotations.EntityType;

/**
 * Base interface for all EHRI framed vertex types.
 */
public interface Frame extends VertexFrame {

    /**
     * Get the unique item id.
     * @return id
     */
    @Property(EntityType.ID_KEY)
    public String getId();

    /**
     * Get the type key for this frame.
     * @return type
     */
    @Property(EntityType.TYPE_KEY)
    public String getType();
}
