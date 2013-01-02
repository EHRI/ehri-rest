package eu.ehri.project.persistance.utils;

import java.util.List;
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
        return getPathSections(bundle, BundlePath.fromString(path));
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
        return updatePathSections(bundle, BundlePath.fromString(path), value);
    }

    /**
     * Xpath-like method to fetch a set of nested relations.
     * 
     * @param bundle
     * @param path
     * @return
     */
    public static List<Bundle> getRelations(Bundle bundle, String path) {
        return getRelationsPathSections(bundle, BundlePath.fromString(path));
    }

    // Private implementation helpers.

    private static List<Bundle> getRelationsPathSections(Bundle bundle,
            BundlePath path) {
        if (path.isEmpty()) {
            return bundle.getRelations(path.getTerminus());
        } else {
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            try {
                List<Bundle> relations = bundle.getRelations(section.getPath());
                return getRelationsPathSections(relations.get(section.getIndex()), path.next());
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s' not found", section.getIndex()));
            }
        }
    }

    private static Object getPathSections(Bundle bundle, BundlePath path) {
        if (path.isEmpty()) {
            return bundle.getDataValue(path.getTerminus());
        } else {
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            try {
                List<Bundle> relations = bundle.getRelations(section.getPath());
                return getPathSections(relations.get(section.getIndex()), path.next());
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s' not found", section.getIndex()));
            }
        }
    }

    private static Bundle updatePathSections(Bundle bundle, BundlePath path, Object value) {
        if (path.isEmpty()) {
            return bundle.withDataValue(path.getTerminus(), value);
        } else {
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            ListMultimap<String, Bundle> allRelations = LinkedListMultimap
                    .create(bundle.getRelations());
            try {
                List<Bundle> relations = Lists.newLinkedList(allRelations
                        .removeAll(section.getPath()));
                relations.set(
                        section.getIndex(),
                        updatePathSections(relations.get(section.getIndex()), path.next(),
                                value));
                allRelations.putAll(section.getPath(), relations);
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s' not found", section.getIndex()));
            }

            return bundle.withRelations(allRelations);
        }
    }
}
