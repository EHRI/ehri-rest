package eu.ehri.project.core.utils;

import java.lang.reflect.Method;

import com.tinkerpop.frames.Adjacency;

import eu.ehri.project.models.annotations.*;

public class AnnotationUtils {

    public boolean isFetchMethod(Method method) {
        return method.getAnnotation(Fetch.class) != null;            
    }
    
    public String getAdjacencyLabel(Method method) {
        Adjacency ann = method.getAnnotation(Adjacency.class);
        if (ann != null)
            return ann.label();
        return null;    // Argh, unchecked nulls!
    }
}
