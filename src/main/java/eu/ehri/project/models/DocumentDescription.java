package eu.ehri.project.models;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;

@EntityType(EntityTypes.DOCUMENT_DESCRIPTION)
public interface DocumentDescription extends VertexFrame, Description {

    @Property("title")
    public String getTitle();

    @Property("identifier")
    public String getIdentifier();

    @Property("scopeAndContent")
    public String getScopeAndContent();

    @Property("languageOfDescription")
    public String getLanguageOfDescription();

}
