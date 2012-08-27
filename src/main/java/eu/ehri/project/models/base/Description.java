package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface Description extends VertexFrame, VersionedEntity {
    public static final String DESCRIBES = "describes";

    @Adjacency(label = DESCRIBES)
    public DescribedEntity getEntity();
    
    @Property("languageCode")
    public String getLanguageOfDescription();

}
