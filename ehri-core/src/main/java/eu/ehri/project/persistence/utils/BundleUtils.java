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

package eu.ehri.project.persistence.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.persistence.Bundle;

import java.util.List;
import java.util.Map;

/**
 * Helpers for working with the {@link Bundle} format.
 */
public class BundleUtils {

    private interface SetOperation {
        Bundle run(Bundle bundle, BundlePath path);
    }

    private interface GetOperation<T> {
        T run(Bundle bundle, BundlePath path);
    }

    public static class BundlePathError extends NullPointerException {
        public BundlePathError(String message) {
            super(message);
        }
    }

    public static class BundleIndexError extends IndexOutOfBoundsException {
        public BundleIndexError(String message) {
            super(message);
        }
    }

    /**
     * XPath-like method for getting the value of a nested relation's attribute.
     * <p>
     * <pre>
     * {@code
     * String lang = BundleUtils.get(bundle, "describes[0]/languageCode"));
     * }
     * </pre>
     *
     * @param bundle the bundle
     * @param path   a path string
     * @param <T>    the type to fetch
     * @return a property of type T
     */
    public static <T> T get(Bundle bundle, String path) {
        return fetchAttribute(bundle, BundlePath.fromString(path),
                (subjectNode, subjectPath) -> subjectNode.getDataValue(subjectPath
                        .getTerminus()));
    }

    /**
     * XPath-like method for getting a bundle at a given path.
     * <p>
     * <pre>
     * {@code
     * Bundle description = BundleUtils.getBundle(bundle, "describes[0]"));
     * }
     * </pre>
     *
     * @param bundle the bundle
     * @param path   a path string
     * @return a bundle found at the given path
     */
    public static Bundle getBundle(Bundle bundle, String path) {
        return fetchNode(bundle, BundlePath.fromString(path));
    }

    /**
     * XPath-like method for deleting the value of a nested relation's
     * attribute.
     * <p>
     * <pre>
     * {@code
     * Bundle newBundle = BundleUtils.delete(bundle, "describes[0]/languageCode"));
     * }
     * </pre>
     *
     * @param bundle the bundle
     * @param path   a path string
     * @return the bundle with the item at the given path deleted
     */
    public static Bundle delete(Bundle bundle, String path) {
        return mutateAttribute(bundle, BundlePath.fromString(path),
                (subject, p) -> {
                    Map<String, Object> data = Maps.newHashMap(subject
                            .getData());
                    data.remove(p.getTerminus());
                    return subject.withData(data);
                });
    }


    /**
     * XPath-like method for deleting a node from a nested tree, i.e:
     * <p>
     * <pre>
     * {@code
     * Bundle newBundle = BundleUtils.deleteBundle(bundle, "describes[0]"));
     * }
     * </pre>
     *
     * @param bundle the bundle
     * @param path   a path string
     * @return the bundle with the bundle at the given path deleted
     */
    public static Bundle deleteBundle(Bundle bundle, String path) {
        return deleteNode(bundle, BundlePath.fromString(path));
    }

    /**
     * Xpath-like method for creating a new bundle by updating a nested relation
     * of an existing bundle.
     * <p>
     * <pre>
     * {@code
     * Bundle newBundle = BundleUtils.set(oldBundle, "hasDate[0]/startDate", "1923-10-10");
     * }
     * </pre>
     *
     * @param bundle the bundle
     * @param path   a path string
     * @param value  the value being set
     * @param <T>    the type of property being set
     * @return the bundle with the property at the given path set
     */
    public static <T> Bundle set(Bundle bundle, String path, final T value) {
        return mutateAttribute(bundle, BundlePath.fromString(path),
                (subject, p) -> subject.withDataValue(p.getTerminus(), value));
    }

    /**
     * Xpath-like method for creating a new bundle by updating a nested relation
     * of an existing bundle.
     * <p>
     * <pre>
     * {@code
     * Bundle dateBundle = ...
     * Bundle newBundle = BundleUtils.setBundle(oldBundle, "hasDate[0]", dateBundle);
     * }
     * </pre>
     *
     * @param bundle    the bundle
     * @param path      a path string
     * @param newBundle the new bundle to set at the path
     * @return a bundle with the given bundle set at the given path
     */
    public static Bundle setBundle(Bundle bundle, String path, Bundle newBundle) {
        return setNode(bundle, BundlePath.fromString(path), newBundle);
    }

    /**
     * Xpath-like method to fetch a set of nested relations.
     * <p>
     * <pre>
     * {@code
     * List<Bundle> dates = BundleUtils.getRelations(bundle, "hasDate");
     * }
     * </pre>
     *
     * @param bundle the bundle
     * @param path   a path string
     * @return a list of bundles at the given relationship path
     */
    public static List<Bundle> getRelations(Bundle bundle, String path) {
        return fetchAttribute(bundle, BundlePath.fromString(path),
                (subjectNode, subjectPath) -> subjectNode.getRelations(subjectPath
                        .getTerminus()));
    }

    // Private implementation helpers.

    private static <T> T fetchAttribute(Bundle bundle, BundlePath path,
            GetOperation<T> op) {
        if (path.isEmpty()) {
            return op.run(bundle, path);
        } else {
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            try {
                List<Bundle> relations = bundle.getRelations(section.getPath());
                return fetchAttribute(relations.get(section.getIndex()),
                        path.next(), op);
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s[%s]' not found", path.next(),
                        section.getIndex()));
            }
        }
    }

    private static Bundle fetchNode(Bundle bundle, BundlePath path) {
        if (path.hasTerminus())
            throw new IllegalArgumentException(
                    "Last component of path must be a valid subtree address.");

        if (path.isEmpty()) {
            return bundle;
        } else {
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            List<Bundle> relations = bundle.getRelations(section.getPath());
            try {
                Bundle next = relations.get(section.getIndex());
                return fetchNode(next, path.next());
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s[%s]' not found: %s", section.getPath(),
                        section.getIndex(), relations));
            }
        }
    }

    private static Bundle mutateAttribute(Bundle bundle, BundlePath path,
            SetOperation op) {
        // Case one: the main path is empty, so we *only* run
        // the op on the top-level node.
        if (path.isEmpty()) {
            return op.run(bundle, path);
        } else {
            // Case two:            
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            ListMultimap<String, Bundle> allRelations = ArrayListMultimap
                    .create(bundle.getRelations());
            try {
                List<Bundle> relations = Lists.newArrayList(allRelations
                        .removeAll(section.getPath()));
                Bundle subject = relations.get(section.getIndex());
                relations.set(section.getIndex(),
                        mutateAttribute(subject, path.next(), op));
                allRelations.putAll(section.getPath(), relations);
                return bundle.replaceRelations(allRelations);
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s[%s]' not found", section.getPath(),
                        section.getIndex()));
            }
        }
    }

    private static Bundle setNode(Bundle bundle, BundlePath path, Bundle newNode) {
        if (path.hasTerminus())
            throw new IllegalArgumentException(
                    "Last component of path must be a valid subtree address.");
        if (path.isEmpty())
            throw new IllegalArgumentException("Path must refer to a nested node.");

        PathSection section = path.current();
        BundlePath next = path.next();

        if (section.getIndex() != -1 && !bundle.hasRelations(section.getPath())) {
            throw new BundlePathError(String.format(
                    "Relation path '%s' not found", section.getPath()));
        }
        ListMultimap<String, Bundle> allRelations = ArrayListMultimap
                .create(bundle.getRelations());
        try {
            List<Bundle> relations = Lists.newArrayList(allRelations
                    .removeAll(section.getPath()));
            if (next.isEmpty()) {
                // If the index is negative, add to the end...
                if (section.getIndex() == -1) {
                    relations.add(newNode);
                } else {
                    relations.set(section.getIndex(), newNode);
                }
            } else {
                Bundle subject = relations.get(section.getIndex());
                relations.set(section.getIndex(),
                        setNode(subject, next, newNode));
            }
            allRelations.putAll(section.getPath(), relations);
            return bundle.replaceRelations(allRelations);
        } catch (IndexOutOfBoundsException e) {
            throw new BundleIndexError(String.format(
                    "Relation index '%s[%s]' not found", next.current().getPath(),
                    next.current().getIndex()));
        }
    }

    private static Bundle deleteNode(Bundle bundle, BundlePath path) {
        if (path.hasTerminus())
            throw new IllegalArgumentException(
                    "Last component of path must be a valid subtree address.");
        if (path.isEmpty())
            throw new IllegalArgumentException("Path must refer to a nested node.");

        PathSection section = path.current();
        BundlePath next = path.next();

        if (!bundle.hasRelations(section.getPath()))
            throw new BundlePathError(String.format(
                    "Relation path '%s' not found", section.getPath()));
        ListMultimap<String, Bundle> allRelations = ArrayListMultimap
                .create(bundle.getRelations());
        try {
            List<Bundle> relations = Lists.newArrayList(allRelations
                    .removeAll(section.getPath()));
            if (next.isEmpty()) {
                relations.remove(section.getIndex());
            } else {
                Bundle subject = relations.get(section.getIndex());
                relations.set(section.getIndex(),
                        deleteNode(subject, next));
            }
            if (!relations.isEmpty())
                allRelations.putAll(section.getPath(), relations);
            return bundle.replaceRelations(allRelations);
        } catch (IndexOutOfBoundsException e) {
            throw new BundleIndexError(String.format(
                    "Relation index '%s[%s]' not found", section.getPath(),
                    section.getIndex()));
        }
    }
}
