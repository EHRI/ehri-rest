package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.Annotation;

public interface AnnotatableEntity extends Frame {
    @Adjacency(label = Ontology.ANNOTATION_ANNOTATES, direction = Direction.IN)
    public Iterable<Annotation> getAnnotations();

    @Adjacency(label = Ontology.ANNOTATION_ANNOTATES, direction = Direction.IN)
    public void addAnnotation(final Annotation annotation);
}
