package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface CollectionDescription {

    @Adjacency(label = "describes")
    public Collection getCollection();

    @Property("title")
    public String getTitle();

    @Property("identifier")
    public String getIdentifier();

    @Property("languageOfDescription")
    public String getLanguageOfDescription();

}
