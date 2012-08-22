package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;

public interface Agent extends AccessibleEntity, DescribedEntity,
        AnnotatableEntity {

    public static final String HOLDS = "holds";

    @Adjacency(label = HOLDS)
    public Iterable<DocumentaryUnit> getCollections();

    @Adjacency(label = HOLDS)
    public void addCollection(final TemporalEntity collection);
}
