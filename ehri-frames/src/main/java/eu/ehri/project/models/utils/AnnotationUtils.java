package eu.ehri.project.models.utils;

import java.lang.reflect.Method;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.annotations.Fetch;

public class AnnotationUtils {

    public static boolean isFetchMethod(Method method) {
        return method.getAnnotation(Fetch.class) != null;
    }

    public static String getAdjacencyLabel(Method method) {
        Adjacency ann = method.getAnnotation(Adjacency.class);
        if (ann != null)
            return ann.label();
        return null; // Argh, unchecked nulls!
    }
}
