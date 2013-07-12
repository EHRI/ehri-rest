package eu.ehri.project.models.utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.*;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Helper functions for managing EntityType classes.
 *
 * @author mike
 */
public class ClassUtils {

    public static final String FETCH_METHOD_PREFIX = "get";

    private static final Logger logger = LoggerFactory.getLogger(ClassUtils.class);

    private static Map<Class<?>,Map<String,Method>> fetchMethodCache = Maps.newHashMap();
    private static Map<Class<?>,Iterable<String>> propertyKeysCache = Maps.newHashMap();
    private static Map<Class<?>,Iterable<String>> mandatoryPropertyKeysCache = Maps.newHashMap();
    private static Map<Class<?>,Iterable<String>> uniquePropertyKeysCache = Maps.newHashMap();
    private static Map<Class<?>,Map<String, Direction>> dependentRelationsCache = Maps.newHashMap();
    private static Map<Class<?>,EntityClass> entityClassCache = Maps.newHashMap();

    /**
     * Get the entity type string for a given class.
     *
     * @param cls
     * @return
     */
    public static EntityClass getEntityType(Class<?> cls) {
        if (!entityClassCache.containsKey(cls)) {
            entityClassCache.put(cls, getEntityTypeInternal(cls));
        }
        return entityClassCache.get(cls);
    }

    public static Map<String,Direction> getDependentRelations(Class<?> cls) {
        if (!dependentRelationsCache.containsKey(cls)) {
            dependentRelationsCache.put(cls, getDependentRelationsInternal(cls));
        }
        return dependentRelationsCache.get(cls);
    }

    public static Map<String, Method> getFetchMethods(Class<?> cls) {
        if (!fetchMethodCache.containsKey(cls)) {
            fetchMethodCache.put(cls, getFetchMethodsInternal(cls));
        }
        return fetchMethodCache.get(cls);
    }

    public static Iterable<String> getPropertyKeys(Class<?> cls) {
        if (!propertyKeysCache.containsKey(cls)) {
            propertyKeysCache.put(cls, getPropertyKeysInternal(cls));
        }
        return propertyKeysCache.get(cls);
    }

    public static Iterable<String> getMandatoryPropertyKeys(Class<?> cls) {
        if (!mandatoryPropertyKeysCache.containsKey(cls)) {
            mandatoryPropertyKeysCache.put(cls, getMandatoryPropertyKeysInternal(cls));
        }
        return mandatoryPropertyKeysCache.get(cls);
    }

    public static Iterable<String> getUniquePropertyKeys(Class<?> cls) {
        if (!uniquePropertyKeysCache.containsKey(cls)) {
            uniquePropertyKeysCache.put(cls, getUniquePropertyKeysInternal(cls));
        }
        return uniquePropertyKeysCache.get(cls);
    }

    private static EntityClass getEntityTypeInternal(Class<?> cls) {
        EntityType ann = cls.getAnnotation(EntityType.class);
        if (ann == null)
            throw new RuntimeException(String.format(
                    "Programming error! Bad bundle type: %s", cls.getName()));
        return ann.value();
    }

    private static Map<String, Direction> getDependentRelationsInternal(Class<?> cls) {
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

    private static Map<String, Method> getFetchMethodsInternal(Class<?> cls) {
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
            out.putAll(getFetchMethodsInternal(s));
        }
        return out;
    }

    private static Iterable<String> getPropertyKeysInternal(Class<?> cls) {
        List<String> out = Lists.newLinkedList();
        for (Method method : cls.getMethods()) {
            Property ann = method.getAnnotation(Property.class);
            if (ann != null)
                out.add(ann.value());
        }

        for (Class<?> s : cls.getInterfaces()) {
            Iterables.addAll(out, getPropertyKeysInternal(s));
        }

        return ImmutableSet.copyOf(out);
    }

    private static Iterable<String> getMandatoryPropertyKeysInternal(Class<?> cls) {
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
            Iterables.addAll(out, getMandatoryPropertyKeysInternal(s));
        }

        return ImmutableSet.copyOf(out);
    }

    private static Iterable<String> getUniquePropertyKeysInternal(Class<?> cls) {
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
            Iterables.addAll(out, getUniquePropertyKeysInternal(s));
        }

        return ImmutableSet.copyOf(out);
    }
}
