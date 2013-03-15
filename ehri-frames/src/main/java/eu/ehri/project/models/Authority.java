package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.AnnotatableEntity;
import eu.ehri.project.models.base.DescribedEntity;

@EntityType(EntityClass.AUTHORITY)
public interface Authority extends VertexFrame, AccessibleEntity,
        DescribedEntity, AnnotatableEntity {

    public static final String CREATED = "created";
}
