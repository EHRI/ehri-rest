package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.Annotation;

public interface Annotator extends VertexFrame {
    public static final String HAS_ANNOTATION = "hasAnnotation";
    
    @Adjacency(label = HAS_ANNOTATION)
    public Iterable<Annotation> getAnnotations();
}
