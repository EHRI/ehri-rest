package eu.ehri.project.persistance.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.helpers.collection.Iterables;

import com.google.common.base.Splitter;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import eu.ehri.project.persistance.Bundle;

/**
 * Helpers for working with the bundle format.
 * 
 * @author michaelb
 * 
 */
public class BundleUtils {

    public static final String PATH_SEP = "/";
    private static final Pattern pattern = Pattern
            .compile("([^/\\[\\]]+)\\[(\\d+)\\]");
    private static Splitter splitter = Splitter.on(PATH_SEP).omitEmptyStrings();

    public static class BundlePathError extends NullPointerException {
        private static final long serialVersionUID = -8933938027614207655L;

        public BundlePathError(String message) {
            super(message);
        }
    }

    public static class BundleIndexError extends IndexOutOfBoundsException {
        private static final long serialVersionUID = -7959054156406534135L;

        public BundleIndexError(String message) {
            super(message);
        }
    }

    /**
     * XPath-like method for getting the value of a nested relation's attribute,
     * i.e:
     * 
     * String lang = BundleUtils.get(bundle, "describes[0]/languageCode"));
     * 
     * @param bundle
     * @param path
     * @return
     */
    public static Object get(Bundle bundle, String path) {
        List<String> list = Iterables.toList(splitter.split(path));
        String attr = list.remove(list.size() - 1);

        return getPathSections(bundle, list, attr);
    }

    /**
     * Xpath-like method for creating a new bundle by updating a nested relation
     * of an existing bundle, i.e:
     * 
     * Bundle newBundle = BundleUtils.set(oldBundle, "name", "Foobar"); Bundle
     * newBundle = BundleUtils.set(oldBundle, "hasDate[0]/startDate",
     * "1923-10-10");
     * 
     * 
     * @param bundle
     * @return
     */
    public static Bundle set(Bundle bundle, String path, Object value) {
        List<String> list = Iterables.toList(splitter.split(path));
        String attr = list.remove(list.size() - 1);

        return updatePathSections(bundle, list, attr, value);
    }

    /**
     * Xpath-like method to fetch a set of nested relations.
     * 
     * @param bundle
     * @param path
     * @return
     */
    public static List<Bundle> getRelations(Bundle bundle, String path) {
        List<String> list = Iterables.toList(splitter.split(path));
        String attr = list.remove(list.size() - 1);

        return getRelationsPathSections(bundle, list, attr);
    }

    // Private implementation helpers.

    private static List<Bundle> getRelationsPathSections(Bundle bundle,
            List<String> list, String attr) {
        if (list.isEmpty()) {
            return bundle.getRelations(attr);
        } else {
            String pathSection = list.remove(0);
            Matcher matcher = pattern.matcher(pathSection);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Bad path section for nested bundle update: '%s'. "
                                        + "Non-terminal paths should contain relation name and index.",
                                pathSection));
            }
            String relation = matcher.group(1);
            Integer index = Integer.valueOf(matcher.group(2));

            if (!bundle.hasRelations(relation))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", pathSection));
            try {
                List<Bundle> relations = bundle.getRelations(relation);
                return getRelationsPathSections(relations.get(index), list,
                        attr);
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s' not found", pathSection));
            }
        }
    }

    private static Object getPathSections(Bundle bundle, List<String> list,
            String attr) {
        if (list.isEmpty()) {
            return bundle.getDataValue(attr);
        } else {
            String pathSection = list.remove(0);
            Matcher matcher = pattern.matcher(pathSection);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Bad path section for nested bundle update: '%s'. "
                                        + "Non-terminal paths should contain relation name and index.",
                                pathSection));
            }
            String relation = matcher.group(1);
            Integer index = Integer.valueOf(matcher.group(2));

            if (!bundle.hasRelations(relation))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", pathSection));
            try {
                List<Bundle> relations = bundle.getRelations(relation);
                return getPathSections(relations.get(index), list, attr);
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s' not found", pathSection));
            }
        }
    }

    private static Bundle updatePathSections(Bundle bundle, List<String> list,
            String attr, Object value) {
        if (list.isEmpty()) {
            return bundle.withDataValue(attr, value);
        } else {
            String pathSection = list.remove(0);
            Matcher matcher = pattern.matcher(pathSection);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Bad path section for nested bundle update: '%s'. "
                                        + "Non-terminal paths should contain relation name and index.",
                                pathSection));
            }
            String relation = matcher.group(1);
            Integer index = Integer.valueOf(matcher.group(2));

            if (!bundle.hasRelations(relation))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", pathSection));
            ListMultimap<String, Bundle> allRelations = LinkedListMultimap
                    .create(bundle.getRelations());
            try {
                List<Bundle> relations = Lists.newLinkedList(allRelations
                        .removeAll(relation));
                relations.set(
                        index,
                        updatePathSections(relations.get(index), list, attr,
                                value));
                allRelations.putAll(relation, relations);
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s' not found", pathSection));
            }

            return bundle.withRelations(allRelations);
        }
    }
}
