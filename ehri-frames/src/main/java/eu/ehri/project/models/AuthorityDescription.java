package eu.ehri.project.models;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityEnumType;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;

@EntityType(EntityTypes.AUTHORITY_DESCRIPTION)
@EntityEnumType(EntityEnumTypes.AUTHORITY_DESCRIPTION)
public interface AuthorityDescription extends VertexFrame, Description {

    @Property("title")
    public String getTitle();
}
