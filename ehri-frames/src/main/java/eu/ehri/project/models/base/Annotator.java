package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.Annotation;

public interface Annotator extends Frame {
    public static final String HAS_ANNOTATION = "hasAnnotation";

    @Adjacency(label = HAS_ANNOTATION)
    public Iterable<Annotation> getAnnotations();
}
