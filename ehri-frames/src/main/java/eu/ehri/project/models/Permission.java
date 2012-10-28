package eu.ehri.project.models;

import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;

@EntityType(EntityTypes.PERMISSION)
public interface Permission extends AccessibleEntity {
    public static final String MASK = "mask";
    
    @Property(MASK)
    public int getMask();
}
