package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.events.Action;

@EntityType(EntityClass.IMPORT)
public interface Import extends Action, AccessibleEntity, VertexFrame {

    @Adjacency(label = HAS_SUBJECT)
    public Iterable<AccessibleEntity> getItems();

    @Adjacency(label = HAS_SUBJECT)
    public void addItem(final AccessibleEntity item);
}
