package eu.ehri.project.models;

import com.tinkerpop.frames.Property;

public interface AgentDescription extends Description {

    @Property("title")
    public String getTitle();

    @Property("otherFormsOfName")
    public String[] otherFormsOfName();

    @Property("parallelFormsOfName")
    public String[] parallelFormsOfName();

    @Property("identifier")
    public String getIdentifier();

    @Property("languageOfDescription")
    public String getLanguageOfDescription();
}
