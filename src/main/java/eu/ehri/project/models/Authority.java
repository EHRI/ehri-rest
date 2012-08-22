package eu.ehri.project.models;

import com.tinkerpop.frames.Property;

interface Authority extends AccessibleEntity, DescribedEntity,
        AnnotatableEntity {
    
    public static final String isA = "authority";
    
    @Property("type_of_entity")
    public String getTypeOfEntity();
}
