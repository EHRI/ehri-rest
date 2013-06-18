package eu.ehri.project.persistance.utils;

import java.util.List;
import java.util.Map;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.ehri.project.persistance.Bundle;

/**
 * Helpers for working with the bundle format.
 * 
 * @author michaelb
 * 
 */
public class BundleUtils {

    private static interface SetOperation {
        public Bundle run(final Bundle bundle, final BundlePath path);
    }

    private static interface GetOperation<T> {
        public T run(final Bundle bundle, final BundlePath path);
    }

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
        return fetchAttribute(bundle, BundlePath.fromString(path),
                new GetOperation<Object>() {
                    public Object run(final Bundle subjectNode,
                            BundlePath subjectPath) {
                        return subjectNode.getDataValue(subjectPath
                                .getTerminus());
                    }
                });
    }

    /**
     * XPath-like method for getting a node at a given path.
     * i.e:
     * 
     * String lang = BundleUtils.get(bundle, "describes[0]/languageCode"));
     * 
     * @param bundle
     * @param path
     * @return
     */
    public static Bundle getBundle(Bundle bundle, String path) {
        return fetchNode(bundle, BundlePath.fromString(path));
    }

    /**
     * XPath-like method for deleting the value of a nested relation's
     * attribute, i.e:
     * 
     * Bundle newBundle = BundleUtils.delete(bundle,
     * "describes[0]/languageCode"));
     * 
     * @param bundle
     * @param path
     * @return
     */
    public static Bundle delete(Bundle bundle, String path) {
        return mutateAttribute(bundle, BundlePath.fromString(path),
                new SetOperation() {
                    public Bundle run(final Bundle subject, final BundlePath p) {
                        Map<String, Object> data = Maps.newHashMap(subject
                                .getData());
                        data.remove(p.getTerminus());
                        return subject.withData(data);
                    }
                });
    }

    
    /**
     * XPath-like method for deleting a node from a nested tree, i.e:
     * 
     * Bundle newBundle = BundleUtils.delete(bundle,
     * "describes[0]/languageCode"));
     * 
     * @param bundle
     * @param path
     * @return
     */
    public static Bundle deleteBundle(Bundle bundle, String path) {
        return deleteNode(bundle, BundlePath.fromString(path));
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
    public static Bundle set(Bundle bundle, String path, final Object value) {
        return mutateAttribute(bundle, BundlePath.fromString(path),
                new SetOperation() {
                    public Bundle run(final Bundle subject, final BundlePath p) {
                        return subject.withDataValue(p.getTerminus(), value);
                    }
                });
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
    public static Bundle setBundle(Bundle bundle, String path, Bundle newBundle) {
        return setNode(bundle, BundlePath.fromString(path), newBundle);
    }

    /**
     * Xpath-like method to fetch a set of nested relations.
     * 
     * @param bundle
     * @param path
     * @return
     */
    public static List<Bundle> getRelations(Bundle bundle, String path) {
        return fetchAttribute(bundle, BundlePath.fromString(path),
                new GetOperation<List<Bundle>>() {
                    public List<Bundle> run(final Bundle subjectNode,
                            BundlePath subjectPath) {
                        return subjectNode.getRelations(subjectPath
                                .getTerminus());
                    }
                });
    }

    // Private implementation helpers.

    /**
     * Perform 'fetch' queries on the tree, returning type T.
     * 
     * @param bundle
     * @param path
     * @param op
     * @return
     */
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
                        "Relation index '%s[%s]' not found", section.getPath(),
                        section.getIndex()));
            }
        }
    }

    /**
     * Fetch a bundle node at the given path, which must end with a valid
     * relationship name and index.
     * 
     * @param bundle
     * @param path
     * @return
     */
    private static Bundle fetchNode(Bundle bundle, BundlePath path) {
        if (path.hasTerminus())
            throw new IllegalArgumentException(
                    "Last component of path must be a valid subtree address.");
        if (path.isEmpty()) {
            return fetchNode(bundle, path.next());
        } else {
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            try {
                List<Bundle> relations = bundle.getRelations(section.getPath());
                return relations.get(section.getIndex());
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s[%s]' not found", section.getPath(),
                        section.getIndex()));
            }
        }
    }

    /**
     * Perform mutating operations on the tree, returning an immutable copy.
     * 
     * @param bundle
     * @param path
     * @param op
     * @return
     */
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
            ListMultimap<String, Bundle> allRelations = LinkedListMultimap
                    .create(bundle.getRelations());
            try {
                List<Bundle> relations = Lists.newLinkedList(allRelations
                        .removeAll(section.getPath()));
                Bundle subject = relations.get(section.getIndex());
                relations.set(section.getIndex(),
                        mutateAttribute(subject, path.next(), op));
                allRelations.putAll(section.getPath(), relations);
                return bundle.withRelations(allRelations);
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s[%s]' not found", section.getPath(),
                        section.getIndex()));
            }
        }
    }

    /**
     * Set a nested node in the tree, returning an immutable copy.
     * 
     * @param bundle
     * @param path
     * @param newNode
     * @return
     */
    private static Bundle setNode(Bundle bundle, BundlePath path, Bundle newNode) {
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
        ListMultimap<String, Bundle> allRelations = LinkedListMultimap
                .create(bundle.getRelations());
        try {
            List<Bundle> relations = Lists.newLinkedList(allRelations
                    .removeAll(section.getPath()));
            if (next.isEmpty()) {
                relations.set(section.getIndex(), newNode);
            } else {
                Bundle subject = relations.get(section.getIndex());
                relations.set(section.getIndex(),
                        setNode(subject, next, newNode));
            }
            allRelations.putAll(section.getPath(), relations);
            return bundle.withRelations(allRelations);
        } catch (IndexOutOfBoundsException e) {
            throw new BundleIndexError(String.format(
                    "Relation index '%s[%s]' not found", section.getPath(),
                    section.getIndex()));
        }
    }
    
    /**
     * Delete a nested node from the tree, returning an immutable copy.
     * 
     * @param bundle
     * @param path
     * @param op
     * @return
     */
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
        ListMultimap<String, Bundle> allRelations = LinkedListMultimap
                .create(bundle.getRelations());
        try {
            List<Bundle> relations = Lists.newLinkedList(allRelations
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
            return bundle.withRelations(allRelations);
        } catch (IndexOutOfBoundsException e) {
            throw new BundleIndexError(String.format(
                    "Relation index '%s[%s]' not found", section.getPath(),
                    section.getIndex()));
        }
    }    
}
