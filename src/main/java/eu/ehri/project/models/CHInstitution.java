package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;

public interface CHInstitution extends Entity {

    @Adjacency(label = "holds")
    public Iterable<Collection> getCollections();

    @Adjacency(label = "holds")
    public void addCollection(final Collection collection);

    @Adjacency(label = "describes")
    public Iterable<CollectionDescription> getDescriptions();
}
