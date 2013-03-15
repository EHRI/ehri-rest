package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.Annotation;

public interface AnnotatableEntity extends VertexFrame {
    @Adjacency(label = Annotation.ANNOTATES, direction = Direction.IN)
    public Iterable<Annotation> getAnnotations();

    @Adjacency(label = Annotation.ANNOTATES, direction = Direction.IN)
    public void addAnnotation(final Annotation annotation);
}
