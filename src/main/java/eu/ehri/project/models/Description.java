package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;

public interface Description {
    public static final String DESCRIBES = "describes";

    @Adjacency(label = DESCRIBES)
    public DescribedEntity getEntity();
}
