package eu.ehri.project.models;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityEnumType;
import eu.ehri.project.models.base.AccessibleEntity;

@EntityEnumType(EntityEnumTypes.REVISION)
public interface Revision extends AccessibleEntity, VertexFrame {

}
