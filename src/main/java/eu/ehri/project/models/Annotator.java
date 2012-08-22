package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;

public interface Annotator {
    public static final String HAS_ANNOTATION = "hasAnnotation";
    
    @Adjacency(label = HAS_ANNOTATION)
    public Iterable<Annotation> getAnnotations();
}
