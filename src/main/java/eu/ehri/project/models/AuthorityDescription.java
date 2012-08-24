package eu.ehri.project.models;

import com.tinkerpop.frames.Property;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;

@EntityType(EntityTypes.AUTHORITY_DESCRIPTION)
public interface AuthorityDescription extends Description {

    @Property("title")
    public String getTitle();

    @Property("functions")
    public String getFunctions();

    @Property("generalContext")
    public String getGeneralContext();

    @Property("history")
    public String getHistory();

    @Property("languageOfDescription")
    public String getLanguageOfDescription();

}
