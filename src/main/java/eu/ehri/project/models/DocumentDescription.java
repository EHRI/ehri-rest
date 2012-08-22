package eu.ehri.project.models;

import com.tinkerpop.frames.Property;

public interface DocumentDescription extends Description {
    public static final String isA = "documentDescription";

    @Property("title")
    public String getTitle();

    @Property("identifier")
    public String getIdentifier();

    @Property("scopeAndContent")
    public String getScopeAndContent();

    @Property("languageOfDescription")
    public String getLanguageOfDescription();

}
