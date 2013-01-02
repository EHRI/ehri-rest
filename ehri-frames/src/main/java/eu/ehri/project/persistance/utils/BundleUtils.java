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
        return fetchByPath(bundle, BundlePath.fromString(path), new GetOperation<Object>() {
            public Object run(final Bundle subjectNode, BundlePath subjectPath) {
                return subjectNode.getDataValue(subjectPath.getTerminus());
            }
        });
    }

    /**
     * XPath-like method for deleting the value of a nested relation's attribute,
     * i.e:
     * 
     * Bundle newBundle = BundleUtils.delete(bundle, "describes[0]/languageCode"));
     * 
     * @param bundle
     * @param path
     * @return
     */
    public static Bundle delete(Bundle bundle, String path) {
        return mutateByPath(bundle, BundlePath.fromString(path), new SetOperation() {
            public Bundle run(final Bundle subject, final BundlePath p) {
                Map<String, Object> data = Maps.newHashMap(subject.getData());
                data.remove(p.getTerminus());
                return subject.withData(data);
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
    public static Bundle set(Bundle bundle, String path, final Object value) {
        return mutateByPath(bundle, BundlePath.fromString(path), new SetOperation() {
            public Bundle run(final Bundle subject, final BundlePath p) {
                return subject.withDataValue(p.getTerminus(), value);
            }
         });
    }

    /**
     * Xpath-like method to fetch a set of nested relations.
     * 
     * @param bundle
     * @param path
     * @return
     */
    public static List<Bundle> getRelations(Bundle bundle, String path) {
        return fetchByPath(bundle, BundlePath.fromString(path), new GetOperation<List<Bundle>>() {
            public List<Bundle> run(final Bundle subjectNode, BundlePath subjectPath) {
                return subjectNode.getRelations(subjectPath.getTerminus());
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
    private static <T> T fetchByPath(Bundle bundle, BundlePath path, GetOperation<T> op) {
        if (path.hasNext()) {
            return op.run(bundle, path);
        } else {
            PathSection section = path.current();
            if (!bundle.hasRelations(section.getPath()))
                throw new BundlePathError(String.format(
                        "Relation path '%s' not found", section.getPath()));
            try {
                List<Bundle> relations = bundle.getRelations(section.getPath());
                return fetchByPath(relations.get(section.getIndex()), path.next(), op);
            } catch (IndexOutOfBoundsException e) {
                throw new BundleIndexError(String.format(
                        "Relation index '%s' not found", section.getIndex()));
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
    private static Bundle mutateByPath(Bundle bundle, BundlePath path, SetOperation op) {
        PathSection section = path.current();
        if (path.hasNext()) {
            return op.run(bundle, path);
        } else {
            BundlePath next = path.next();
            if (next.hasNext()) {
                
                if (!bundle.hasRelations(section.getPath()))
                    throw new BundlePathError(String.format(
                            "Relation path '%s' not found", section.getPath()));
                ListMultimap<String, Bundle> allRelations = LinkedListMultimap
                        .create(bundle.getRelations());
                try {
                    List<Bundle> relations = Lists.newLinkedList(allRelations
                            .removeAll(section.getPath()));
                    Bundle subject = relations.get(section.getIndex());
                    relations.set(section.getIndex(), op.run(subject, next));
                    allRelations.putAll(section.getPath(), relations);
                } catch (IndexOutOfBoundsException e) {
                    throw new BundleIndexError(String.format(
                            "Relation index '%s' not found", section.getIndex()));
                }
    
                return bundle.withRelations(allRelations);
            } else {
                return mutateByPath(bundle, next, op);
            }
        }
    }
}
