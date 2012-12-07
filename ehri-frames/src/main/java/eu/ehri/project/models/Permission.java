package eu.ehri.project.models;

import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityEnumType;
import eu.ehri.project.models.base.AccessibleEntity;

@EntityEnumType(EntityEnumTypes.PERMISSION)
public interface Permission extends AccessibleEntity {
    public static final String MASK = "mask";
    
    @Property(MASK)
    public int getMask();
}
