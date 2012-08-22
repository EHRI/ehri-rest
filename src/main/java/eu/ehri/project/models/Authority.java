package eu.ehri.project.models;

import com.tinkerpop.frames.Property;

interface Authority extends AccessibleEntity, DescribedEntity,
        AnnotatableEntity {
    @Property("type_of_entity")
    public String getTypeOfEntity();
}
