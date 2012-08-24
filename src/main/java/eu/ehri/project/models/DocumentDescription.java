package eu.ehri.project.models;

import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;

@EntityType(EntityTypes.DOCUMENT_DESCRIPTION)
public interface DocumentDescription extends Description {

    @Property("title")
    public String getTitle();

    @Property("identifier")
    public String getIdentifier();

    @Property("scopeAndContent")
    public String getScopeAndContent();

    @Property("languageOfDescription")
    public String getLanguageOfDescription();

}
