package eu.ehri.project.core.utils;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Dependent;

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
    
    public static List<String> getDependentRelations(Class<?> cls) {
        List<String> out = new LinkedList<String>();
        for (Method method : cls.getMethods()) {
            if (method.getAnnotation(Dependent.class) != null) {
                Adjacency ann = method.getAnnotation(Adjacency.class);
                if (ann != null)
                    out.add(ann.label());
            }
        }
        return out;
    }
}
