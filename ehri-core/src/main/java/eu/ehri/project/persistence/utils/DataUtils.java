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
import eu.ehri.project.persistence.NestableData;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Helpers for working with the {@link NestableData} format.
 */
public class DataUtils {

    public static class BundlePathError extends NullPointerException {
        BundlePathError(String message) {
            super(message);
        }
    }

    static class BundleIndexError extends IndexOutOfBoundsException {
        BundleIndexError(String message) {
            super(message);
        }
    }

    /**
     * XPath-like method for getting the value of a nested relation's attribute.
     * <p>
     * <pre>
     * {@code
     * String lang = DataUtils.get(item, "describes[0]/languageCode"));
     * }
     * </pre>
     *
     * @param item the item
     * @param path a path string
     * @param <T>  the type to fetch
     * @return a property of type T
     */
    public static <T, N extends NestableData<N>> T get(N item, String path) {
        return fetchAttribute(item, NestableDataPath.fromString(path),
                (subjectNode, subjectPath) -> subjectNode.getDataValue(subjectPath
                        .getTerminus()));
    }

    /**
     * XPath-like method for getting a item at a given path.
     * <p>
     * <pre>
     * {@code
     * Bundle description = DataUtils.getItem(item, "describes[0]"));
     * }
     * </pre>
     *
     * @param item the item
     * @param path a path string
     * @return a item found at the given path
     */
    public static <N extends NestableData<N>> N getItem(N item, String path) {
        return fetchNode(item, NestableDataPath.fromString(path));
    }

    /**
     * XPath-like method for deleting the value of a nested relation's
     * attribute.
     * <p>
     * <pre>
     * {@code
     * Bundle newBundle = DataUtils.delete(item, "describes[0]/languageCode"));
     * }
     * </pre>
     *
     * @param item the item
     * @param path a path string
     * @return the item with the item at the given path deleted
     */
    public static <N extends NestableData<N>> N delete(N item, String path) {
        return mutateAttribute(item, NestableDataPath.fromString(path),
                (subject, p) -> subject.removeDataValue(p.getTerminus()));
    }


    /**
     * XPath-like method for deleting a node from a nested tree, i.e:
     * <p>
     * <pre>
     * {@code
     * Bundle newBundle = DataUtils.deleteItem(item, "describes[0]"));
     * }
     * </pre>
     *
     * @param item the item
     * @param path a path string
     * @return the item with the item at the given path deleted
     */
    public static <N extends NestableData<N>> N deleteItem(N item, String path) {
        return deleteNode(item, NestableDataPath.fromString(path));
    }

    /**
     * Xpath-like method for creating a new item by updating a nested relation
     * of an existing item.
     * <p>
     * <pre>
     * {@code
     * Bundle newBundle = DataUtils.set(oldBundle, "hasDate[0]/startDate", "1923-10-10");
     * }
     * </pre>
     *
     * @param item  the item
     * @param path  a path string
     * @param value the value being set
     * @param <T>   the type of property being set
     * @return the item with the property at the given path set
     */
    public static <T, N extends NestableData<N>> N set(N item, String path, final T value) {
        return mutateAttribute(item, NestableDataPath.fromString(path),
                (subject, p) -> subject.withDataValue(p.getTerminus(), value));
    }

    /**
     * Xpath-like method for creating a new item by updating a nested relation
     * of an existing item.
     * <p>
     * <pre>
     * {@code
     * Bundle dateBundle = ...
     * Bundle newItem = DataUtils.setItem(oldBundle, "hasDate[0]", dateBundle);
     * }
     * </pre>
     * <p>
     * Use an index of -1 to create a relationship set or append to
     * an existing one.
     *
     * @param item    the item
     * @param path    a path string
     * @param newItem the new item to set at the path
     * @return a item with the given item set at the given path
     */
    public static <N extends NestableData<N>> N setItem(N item, String path, N newItem) {
        return setNode(item, NestableDataPath.fromString(path), newItem);
    }

    /**
     * Xpath-like method to fetch a set of nested relations.
     * <p>
     * <pre>
     * {@code
     * List<Bundle> dates = DataUtils.getRelations(item, "hasDate");
     * }
     * </pre>
     *
     * @param item the item
     * @param path a path string
     * @return a list of bundles at the given relationship path
     */
    public static <N extends NestableData<N>> List<N> getRelations(N item, String path) {
        return fetchAttribute(item, NestableDataPath.fromString(path),
                (subjectNode, subjectPath) -> Lists.newArrayList(subjectNode.getRelations(subjectPath
                        .getTerminus())));
    }

    // Private implementation helpers.

    private static <T, N extends NestableData<N>> T fetchAttribute(N bundle, NestableDataPath path,
            BiFunction<N, NestableDataPath, T> op) {
        if (path.isEmpty()) {
            return op.apply(bundle, path);
        } else {
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            try {
                List<N> relations = bundle.getRelations(section.getPath());
                return fetchAttribute(relations.get(section.getIndex()),
                        path.next(), op);
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s[%s]' not found", path.next(),
                        section.getIndex()));
            }
        }
    }

    private static <N extends NestableData<N>> N fetchNode(N bundle, NestableDataPath path) {
        if (path.getTerminus() == null) {
            throw new IllegalArgumentException(
                    "Last component of path must be a valid subtree address.");
        }
        if (path.isEmpty()) {
            return bundle;
        } else {
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            List<N> relations = bundle.getRelations(section.getPath());
            try {
                N next = relations.get(section.getIndex());
                return fetchNode(next, path.next());
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s[%s]' not found: %s", section.getPath(),
                        section.getIndex(), relations));
            }
        }
    }

    private static <N extends NestableData<N>> N mutateAttribute(N bundle, NestableDataPath path,
            BiFunction<N, NestableDataPath, N> op) {
        // Case one: the main path is empty, so we *only* run
        // the op on the top-level node.
        if (path.isEmpty()) {
            return op.apply(bundle, path);
        } else {
            // Case two:
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            ListMultimap<String, N> allRelations = ArrayListMultimap
                    .create(bundle.getRelations());
            try {
                List<N> relations = Lists.newArrayList(allRelations
                        .removeAll(section.getPath()));
                N subject = relations.get(section.getIndex());
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

    private static <N extends NestableData<N>> N setNode(N bundle, NestableDataPath path, N newNode) {
        if (path.getTerminus() == null) {
            throw new IllegalArgumentException(
                    "Last component of path must be a valid subtree address.");
        }
        if (path.isEmpty())
            throw new IllegalArgumentException("Path must refer to a nested node.");

        PathSection section = path.current();
        NestableDataPath next = path.next();

        if (section.getIndex() != -1 && !bundle.hasRelations(section.getPath())) {
            throw new BundlePathError(String.format(
                    "Relation path '%s' not found", section.getPath()));
        }
        ListMultimap<String, N> allRelations = ArrayListMultimap
                .create(bundle.getRelations());
        try {
            List<N> relations = Lists.newArrayList(allRelations
                    .removeAll(section.getPath()));
            if (next.isEmpty()) {
                // If the index is negative, add to the end...
                if (section.getIndex() == -1) {
                    relations.add(newNode);
                } else {
                    relations.set(section.getIndex(), newNode);
                }
            } else {
                N subject = relations.get(section.getIndex());
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

    private static <N extends NestableData<N>> N deleteNode(N bundle, NestableDataPath path) {
        if (path.getTerminus() == null) {
            throw new IllegalArgumentException(
                    "Last component of path must be a valid subtree address.");
        }
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Path must refer to a nested node.");
        }
        PathSection section = path.current();
        NestableDataPath next = path.next();

        if (!bundle.hasRelations(section.getPath()))
            throw new BundlePathError(String.format(
                    "Relation path '%s' not found", section.getPath()));
        ListMultimap<String, N> allRelations = ArrayListMultimap
                .create(bundle.getRelations());
        try {
            List<N> relations = Lists.newArrayList(allRelations
                    .removeAll(section.getPath()));
            if (next.isEmpty()) {
                relations.remove(section.getIndex());
            } else {
                N subject = relations.get(section.getIndex());
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
