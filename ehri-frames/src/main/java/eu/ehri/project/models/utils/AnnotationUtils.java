package eu.ehri.project.models.utils;

import java.lang.reflect.Method;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.EntityClass;
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
    
    // FIXME: Remove this method
    public static boolean hasFramedInterface(VertexFrame frame, Class<? extends VertexFrame> cls) {
        EntityClass type = ClassUtils.getEntityType(cls);
        if (type != null) {
            String isa = (String) frame.asVertex().getProperty(EntityType.TYPE_KEY);
            return isa.equals(type.getName());
        }
        return false;            
    }
}
