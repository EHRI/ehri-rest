/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.models.utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.annotations.Meta;
import eu.ehri.project.models.annotations.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper functions for managing EntityType classes via reflection.
 */
public class ClassUtils {

    public static final String FETCH_METHOD_PREFIX = "get";

    private static final Logger logger = LoggerFactory.getLogger(ClassUtils.class);

    private static Map<Class<?>,Map<String,Method>> fetchMethodCache = Maps.newHashMap();
    private static Map<Class<?>,Map<String,Method>> metaMethodCache = Maps.newHashMap();
    private static Map<Class<?>,Map<String,Set<String>>> enumPropertyValuesCache = Maps.newHashMap();
    private static Map<Class<?>,Collection<String>> propertyKeysCache = Maps.newHashMap();
    private static Map<Class<?>,Collection<String>> mandatoryPropertyKeysCache = Maps.newHashMap();
    private static Map<Class<?>,Collection<String>> uniquePropertyKeysCache = Maps.newHashMap();
    private static Map<Class<?>,Map<String, Direction>> dependentRelationsCache = Maps.newHashMap();
    private static Map<Class<?>,EntityClass> entityClassCache = Maps.newHashMap();

    /**
     * Get the entity type string for a given class.
     *
     * @param cls the entity type's Java class
     * @return the entity enum
     */
    public static EntityClass getEntityType(Class<?> cls) {
        if (!entityClassCache.containsKey(cls)) {
            entityClassCache.put(cls, getEntityTypeInternal(cls));
        }
        return entityClassCache.get(cls);
    }

    /**
     * Get a map of relationships keyed against their direction for
     * a given class.
     *
     * @param cls the entity's Java class
     * @return a relationship-direction map
     */
    public static Map<String,Direction> getDependentRelations(Class<?> cls) {
        if (!dependentRelationsCache.containsKey(cls)) {
            dependentRelationsCache.put(cls, getDependentRelationsInternal(cls));
        }
        return dependentRelationsCache.get(cls);
    }

    /**
     * Get a map of relationship-names keyed against the method to
     * instantiate them.
     *
     * @param cls the entity's Java class
     * @return a relationship-name-method map
     */
    public static Map<String, Method> getFetchMethods(Class<?> cls) {
        if (!fetchMethodCache.containsKey(cls)) {
            fetchMethodCache.put(cls, getFetchMethodsInternal(cls));
        }
        return fetchMethodCache.get(cls);
    }

    /**
     * Get a map of relationship-names keyed against the method to
     * instantiate them.
     *
     * @param cls the entity's Java class
     * @return a relationship-name-method map
     */
    public static Map<String, Method> getMetaMethods(Class<?> cls) {
        if (!metaMethodCache.containsKey(cls)) {
            metaMethodCache.put(cls, getMetaMethodsInternal(cls));
        }
        return metaMethodCache.get(cls);
    }

    /**
     * Get a collection of names for methods marked as properties.
     *
     * @param cls the entity's Java class
     * @return a collection of property names
     */
    public static Collection<String> getPropertyKeys(Class<?> cls) {
        if (!propertyKeysCache.containsKey(cls)) {
            propertyKeysCache.put(cls, getPropertyKeysInternal(cls));
        }
        return propertyKeysCache.get(cls);
    }

    /**
     * Get a collection of names for methods marked as mandatory properties.
     *
     * @param cls the entity's Java class
     * @return a collection of property names
     */
    public static Collection<String> getMandatoryPropertyKeys(Class<?> cls) {
        if (!mandatoryPropertyKeysCache.containsKey(cls)) {
            mandatoryPropertyKeysCache.put(cls, getMandatoryPropertyKeysInternal(cls));
        }
        return mandatoryPropertyKeysCache.get(cls);
    }

    /**
     * Get a collection of names for methods marked as unique properties.
     *
     * @param cls the entity's Java class
     * @return a collection of property names
     */

    public static Collection<String> getUniquePropertyKeys(Class<?> cls) {
        if (!uniquePropertyKeysCache.containsKey(cls)) {
            uniquePropertyKeysCache.put(cls, getUniquePropertyKeysInternal(cls));
        }
        return uniquePropertyKeysCache.get(cls);
    }

    /**
     * Get a collection of names for methods marked as unique properties.
     *
     * @param cls the entity's Java class
     * @return a collection of property names
     */

    public static Map<String,Set<String>> getEnumPropertyKeys(Class<?> cls) {
        if (!enumPropertyValuesCache.containsKey(cls)) {
            enumPropertyValuesCache.put(cls, getEnumPropertyKeysInternal(cls));
        }
        return enumPropertyValuesCache.get(cls);
    }

    private static Map<String, Set<String>> getEnumPropertyKeysInternal(Class<?> cls) {
        Map<String, Set<String>> out = Maps.newHashMap();
        for (Method method : cls.getMethods()) {
            Property ann = method.getAnnotation(Property.class);
            if (ann != null) {
                String name = ann.value();
                Class<?> returnType = method.getReturnType();
                if (Enum.class.isAssignableFrom(returnType)) {
                    Object[] values = returnType.getEnumConstants();
                    Set<String> strings = Sets.newHashSet();
                    for (Object v : values) {
                        strings.add(v.toString());
                    }
                    out.put(name, strings);
                }
            }
        }
        return out;
    }

    private static EntityClass getEntityTypeInternal(Class<?> cls) {
        EntityType ann = cls.getAnnotation(EntityType.class);
        if (ann == null)
            throw new RuntimeException(String.format(
                    "Programming error! Bad bundle type: %s", cls.getName()));
        return ann.value();
    }

    private static Map<String, Direction> getDependentRelationsInternal(Class<?> cls) {
        Map<String, Direction> out = Maps.newHashMap();
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
        Map<String, Method> out = Maps.newHashMap();
        for (Method method : cls.getMethods()) {
            Fetch fetch = method.getAnnotation(Fetch.class);
            Dependent dep = method.getAnnotation(Dependent.class);
            String value = fetch != null ? fetch.value() : null;
            if ((value != null || dep != null)
                    && method.getName().startsWith(FETCH_METHOD_PREFIX)) {
                out.put(value, method);
                logger.trace(" --- found @Fetch annotation: {}: {}", method.getName(), value);
            }
        }

        for (Class<?> s : cls.getInterfaces()) {
            out.putAll(getFetchMethodsInternal(s));
        }
        return out;
    }

    private static Map<String, Method> getMetaMethodsInternal(Class<?> cls) {
        logger.trace(" - checking for @Meta methods: {}", cls.getCanonicalName());
        Map<String, Method> out = Maps.newHashMap();
        for (Method method : cls.getMethods()) {
            Meta meta = method.getAnnotation(Meta.class);
            String value = meta != null ? meta.value() : null;
            if (value != null) {
                out.put(value, method);
                logger.trace(" --- found @Meta annotation: {}: {}", method.getName(), value);
            }
        }

        for (Class<?> s : cls.getInterfaces()) {
            out.putAll(getMetaMethodsInternal(s));
        }
        return out;
    }

    private static Collection<String> getPropertyKeysInternal(Class<?> cls) {
        List<String> out = Lists.newArrayList();
        for (Method method : cls.getMethods()) {
            Property ann = method.getAnnotation(Property.class);
            if (ann != null)
                out.add(ann.value());
        }

        for (Class<?> s : cls.getInterfaces()) {
            out.addAll(getPropertyKeysInternal(s));
        }

        return ImmutableSet.copyOf(out);
    }

    private static Collection<String> getMandatoryPropertyKeysInternal(Class<?> cls) {
        List<String> out = Lists.newArrayList();
        for (Method method : cls.getMethods()) {
            Mandatory mandatory = method.getAnnotation(Mandatory.class);
            if (mandatory != null) {
                Property ann = method.getAnnotation(Property.class);
                // Ignore structural properties, beginning with '__'
                if (ann != null && !ann.value().startsWith("__")) {
                    out.add(ann.value());
                }
            }

        }

        for (Class<?> s : cls.getInterfaces()) {
            out.addAll(getMandatoryPropertyKeysInternal(s));
        }

        return ImmutableSet.copyOf(out);
    }

    private static Collection<String> getUniquePropertyKeysInternal(Class<?> cls) {
        List<String> out = Lists.newArrayList();
        for (Method method : cls.getMethods()) {
            Unique unique = method.getAnnotation(Unique.class);
            if (unique != null) {
                Property ann = method.getAnnotation(Property.class);
                if (ann != null)
                    out.add(ann.value());
            }

        }

        for (Class<?> s : cls.getInterfaces()) {
            out.addAll(getUniquePropertyKeysInternal(s));
        }

        return ImmutableSet.copyOf(out);
    }
}
