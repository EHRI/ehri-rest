package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.Annotation;

public interface Annotator extends Frame {
    @Adjacency(label = Ontology.ANNOTATOR_HAS_ANNOTATION)
    public Iterable<Annotation> getAnnotations();
}
