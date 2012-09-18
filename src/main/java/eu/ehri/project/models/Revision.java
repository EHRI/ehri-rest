package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.VersionedEntity;

public interface Revision extends AccessibleEntity, VertexFrame {
    
    @Property("revisionSourceType")
    public String getType();
    
    @Property("revisionSourceType")
    public void setType(String type);
    
    @Property("revisionLog")
    public String getLog();
    
    @Property("revisionLog")
    public void setLog(String log);
    
    
    @Adjacency(label=VersionedEntity.HAS_REVISION, direction = Direction.IN)
    public VersionedEntity getCurrentVersion();
}
