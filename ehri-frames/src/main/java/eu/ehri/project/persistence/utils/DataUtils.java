package eu.ehri.project.persistence.utils;

import java.util.Collection;

/**
 * Bag of miscellaneous methods!
 */
public class DataUtils {
    /**
     * Ensure a value isn't an empty array or list, which will
     * cause Neo4j to barf.
     *
     * @param value A unknown object
     * @return If the object is a sequence type, and is empty
     */
    public static boolean isEmptySequence(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof Object[]) {
            return ((Object[]) value).length == 0;
        } else if (value instanceof Collection<?>) {
            return ((Collection) value).isEmpty();
        } else if (value instanceof Iterable<?>) {
            return !((Iterable)value).iterator().hasNext();
        }
        return false;
    }
}