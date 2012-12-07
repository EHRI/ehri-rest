package eu.ehri.project.models;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityEnumType;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;

@EntityType(EntityTypes.DOCUMENT_DESCRIPTION)
@EntityEnumType(EntityEnumTypes.DOCUMENT_DESCRIPTION)
public interface DocumentDescription extends VertexFrame, Description {

    @Property("title")
    public String getTitle();
}
