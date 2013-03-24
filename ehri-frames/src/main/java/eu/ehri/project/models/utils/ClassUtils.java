package eu.ehri.project.models.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.ehri.project.models.annotations.*;
import eu.ehri.project.models.base.Frame;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

import eu.ehri.project.models.EntityClass;

/**
 * Helper functions for managing EntityType classes.
 *
 * @author mike
 */
public class ClassUtils {

    public static final String FETCH_METHOD_PREFIX = "get";

    private static final Logger logger = LoggerFactory.getLogger(ClassUtils.class);

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
            Fetch ann = method.getAnnotation(Fetch.class);
            if (ann != null) {
                out.add(ann.value());
            }
        }
        return out;
    }

    public static Map<String, Method> getFetchMethods(Class<?> cls) {
        logger.trace(" - checking for @Fetch methods: {}", cls.getCanonicalName());
        Map<String, Method> out = new HashMap<String, Method>();
        for (Method method : cls.getMethods()) {
            Fetch fetch = method.getAnnotation(Fetch.class);
            if (fetch != null
                    && method.getName().startsWith(FETCH_METHOD_PREFIX)) {
                out.put(fetch.value(), method);
                logger.trace(" --- found @Fetch annotation: {}: {}", method.getName(), fetch.value());
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

    public static Iterable<String> getPropertyKeys(Class<?> cls) {
        List<String> out = Lists.newLinkedList();
        for (Method method : cls.getMethods()) {
            Property ann = method.getAnnotation(Property.class);
            if (ann != null)
                out.add(ann.value());
        }

        for (Class<?> s : cls.getInterfaces()) {
            Iterables.addAll(out, getPropertyKeys(s));
        }

        return ImmutableSet.copyOf(out);
    }

    public static Iterable<String> getMandatoryPropertyKeys(Class<?> cls) {
        List<String> out = new LinkedList<String>();
        for (Method method : cls.getMethods()) {
            Mandatory mandatory = method.getAnnotation(Mandatory.class);
            if (mandatory != null) {
                Property ann = method.getAnnotation(Property.class);
                if (ann != null)
                    out.add(ann.value());
            }

        }

        for (Class<?> s : cls.getInterfaces()) {
            Iterables.addAll(out, getMandatoryPropertyKeys(s));
        }

        return ImmutableSet.copyOf(out);
    }

    public static Iterable<String> getUniquePropertyKeys(Class<?> cls) {
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
            Iterables.addAll(out, getUniquePropertyKeys(s));
        }

        return ImmutableSet.copyOf(out);
    }

    /**
     * Check if a given vertex is of a particular type.
     *
     * @param frame
     * @param type
     * @return
     */
    public static boolean hasType(Frame frame, EntityClass type) {
        String isa = (String) frame.asVertex().getProperty(EntityType.TYPE_KEY);
        return type.getName().equals(isa);
    }
}
