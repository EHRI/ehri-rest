package eu.ehri.project.models;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;

@EntityType(EntityClass.REVISION)
public interface Revision extends AccessibleEntity, VertexFrame {

}
