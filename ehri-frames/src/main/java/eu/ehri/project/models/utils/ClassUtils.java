package eu.ehri.project.models.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Unique;

/**
 * Helper functions for managing EntityType classes.
 * 
 * @author mike
 * 
 */
public class ClassUtils {

    public static final String FETCH_METHOD_PREFIX = "get";

    /**
     * Get the entity type string for a given class.
     * 
     * @param cls
     * @return
     */
    public static EntityClass getEntityType(Class<?> cls) {
        EntityType ann = cls.getAnnotation(EntityType.class);
        if (ann == null)
            throw new RuntimeException(String.format(
                    "Programming error! Bad bundle type: %s", cls.getName()));
        return ann.value();
    }

    public static Map<String, Direction> getDependentRelations(Class<?> cls) {
        Map<String, Direction> out = new HashMap<String, Direction>();
        for (Method method : cls.getMethods()) {
            if (method.getAnnotation(Dependent.class) != null) {
                Adjacency ann = method.getAnnotation(Adjacency.class);
                if (ann != null)
                    out.put(ann.label(), ann.direction());
            }
        }
        return out;
    }

    public static List<String> getFetchedRelations(Class<?> cls) {
        List<String> out = new LinkedList<String>();
        for (Method method : cls.getMethods()) {
            if (method.getAnnotation(Fetch.class) != null) {
                Adjacency ann = method.getAnnotation(Adjacency.class);
                if (ann != null)
                    out.add(ann.label());
            }
        }
        return out;
    }

    public static Map<String, Method> getFetchMethods(Class<?> cls) {
        Map<String, Method> out = new HashMap<String, Method>();
        for (Method method : cls.getMethods()) {
            if (method.getAnnotation(Fetch.class) != null
                    && method.getName().startsWith(FETCH_METHOD_PREFIX)) {
                Adjacency ann = method.getAnnotation(Adjacency.class);
                if (ann != null)
                    out.put(ann.label(), method);
            }
        }

        for (Class<?> s : cls.getInterfaces()) {
            out.putAll(getFetchMethods(s));
        }

        return out;
    }

    public static Map<String, Method> getDependentMethods(Class<?> cls) {
        Map<String, Method> out = new HashMap<String, Method>();
        for (Method method : cls.getMethods()) {
            if (method.getAnnotation(Dependent.class) != null
                    && method.getName().startsWith(FETCH_METHOD_PREFIX)) {
                Adjacency ann = method.getAnnotation(Adjacency.class);
                if (ann != null)
                    out.put(ann.label(), method);
            }
        }

        for (Class<?> s : cls.getInterfaces()) {
            out.putAll(getDependentMethods(s));
        }

        return out;
    }

    public static List<String> getPropertyKeys(Class<?> cls) {
        List<String> out = new LinkedList<String>();
        for (Method method : cls.getMethods()) {
            Property ann = method.getAnnotation(Property.class);
            if (ann != null)
                out.add(ann.value());
        }

        for (Class<?> s : cls.getInterfaces()) {
            out.addAll(getPropertyKeys(s));
        }

        return makeUnique(out);
    }

    public static List<String> getUniquePropertyKeys(Class<?> cls) {
        List<String> out = new LinkedList<String>();
        for (Method method : cls.getMethods()) {
            Unique unique = method.getAnnotation(Unique.class);
            if (unique != null) {
                Property ann = method.getAnnotation(Property.class);
                if (ann != null)
                    out.add(ann.value());
            }

        }

        for (Class<?> s : cls.getInterfaces()) {
            out.addAll(getUniquePropertyKeys(s));
        }

        return makeUnique(out);
    }

    /**
     * Another method to make a list unique. Sigh.
     * 
     * @param list
     * @return
     */
    public static <T> List<T> makeUnique(List<T> list) {
        List<T> out = new LinkedList<T>();
        HashSet<T> set = new HashSet<T>();
        set.addAll(list);
        out.addAll(set);
        return out;
    }

    /**
     * Check if a given vertex is of a particular type.
     * 
     * @param frame
     * @param type
     * @return
     */
    public static boolean hasType(VertexFrame frame, EntityClass type) {
        String isa = (String) frame.asVertex().getProperty(EntityType.TYPE_KEY);
        return isa.equals(type.getName());
    }
}
