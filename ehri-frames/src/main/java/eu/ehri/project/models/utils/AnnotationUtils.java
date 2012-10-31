package eu.ehri.project.models.utils;

import java.lang.reflect.Method;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityType;
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
    
    public static boolean hasFramedInterface(VertexFrame frame, Class<? extends VertexFrame> cls) {
        String type = ClassUtils.getEntityType(cls);
        if (type != null) {
            String isa = (String) frame.asVertex().getProperty(EntityType.KEY);
            return isa.equals(type);
        }
        return false;            
    }
}
