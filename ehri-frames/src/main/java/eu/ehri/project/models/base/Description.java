package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.Fetch;

public interface Description extends VertexFrame, VersionedEntity {
    public static final String DESCRIBES = "describes";
    public static final String LANGUAGE_CODE = "languageCode";

    @Adjacency(label = DESCRIBES)
    public DescribedEntity getEntity();

    @Property(LANGUAGE_CODE)
    public String getLanguageOfDescription();
    
    /**
     * Get the described entity of a description. This 
     * method if @Fetch serialized only if the description
     * is at the top level of the requested subtree.
     * 
     * @return
     */
    @Fetch(value = DESCRIBES, ifDepth=0)
    @Adjacency(label = DESCRIBES)
    public DescribedEntity getDescribedEntity();    
}
