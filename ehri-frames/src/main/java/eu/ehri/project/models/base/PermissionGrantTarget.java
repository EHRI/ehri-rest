package eu.ehri.project.models.base;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface PermissionGrantTarget extends VertexFrame {
    
    @Property(AccessibleEntity.IDENTIFIER_KEY)
    public String getIdentifier();
}
