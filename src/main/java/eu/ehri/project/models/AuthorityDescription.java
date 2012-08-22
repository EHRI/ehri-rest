package eu.ehri.project.models;

import com.tinkerpop.frames.Property;

public interface AuthorityDescription extends Description {
    public static final String isA = "authorityDescription";

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
