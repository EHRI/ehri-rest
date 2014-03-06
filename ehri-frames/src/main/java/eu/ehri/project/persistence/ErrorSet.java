package eu.ehri.project.persistence;

import com.google.common.collect.*;
import eu.ehri.project.exceptions.SerializationError;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents a set of validation errors associated with a bundle.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class ErrorSet {
    private final Multimap<String, String> errors;
    private final Multimap<String, ErrorSet> relations;

    /**
     * Serialization constant definitions
     */
    public static final String REL_KEY = "relationships";
    public static final String ERROR_KEY = "errors";

    /**
     * Builder class for creating an Error set.
     */
    public static class Builder {
        private final ListMultimap<String, String> errors = ArrayListMultimap.create();
        private final ListMultimap<String, ErrorSet> relations = ArrayListMultimap.create();

        public Builder addError(String key, String error) {
            errors.put(key, error);
            return this;
        }

        public Builder addRelation(String relation, ErrorSet errorSet) {
            relations.put(relation, errorSet);
            return this;
        }

        public ErrorSet build() {
            return new ErrorSet(errors, relations);
        }
    }

    /**
     * Constructor.
     */
    public ErrorSet() {
        this(ArrayListMultimap.<String, String>create(),
                ArrayListMultimap.<String, ErrorSet>create());
    }

    /**
     * Factory constructor.
     *
     * @param key The property key in error
     * @param error The string description of the property error
     */
    public static ErrorSet fromError(String key, String error) {
        LinkedListMultimap<String, String> tmp = LinkedListMultimap.create();
        tmp.put(key, error);
        return new ErrorSet(tmp);
    }

    /**
     * Constructor.
     *
     * @param errors A map of top-level errors
     */
    public ErrorSet(ListMultimap<String, String> errors) {
        this(errors, ArrayListMultimap.<String, ErrorSet>create());
    }

    /**
     * Constructor.
     *
     * @param errors A map of top-level errors
     * @param relations A map of relations
     */
    public ErrorSet(Multimap<String, String> errors, final Multimap<String, ErrorSet> relations) {
        this.errors = ImmutableListMultimap.copyOf(errors);
        this.relations = ImmutableListMultimap.copyOf(relations);
    }

    /**
     * Get errors for a key.
     *
     * @param key A property key
     * @return A list of errors associated with a property key
     */
    public Collection<String> getErrorValue(String key) {
        checkNotNull(key);
        return errors.get(key);
    }

    /**
     * Set a value in the bundle's data.
     *
     * @param key A property key
     * @param err A description of the error
     * @return A new error set
     */
    public ErrorSet withErrorValue(String key, String err) {
        if (err == null) {
            return this;
        } else {
            ListMultimap<String, String> errs = LinkedListMultimap.create(errors);
            errs.put(key, err);
            return withErrors(errs);
        }
    }

    /**
     * Get the top-level errors.
     *
     * @return All top-level errors
     */
    public Multimap<String, String> getErrors() {
        return errors;
    }

    /**
     * Set the entire data map for this bundle.
     *
     * @param errors A set of errors
     * @return A new error set
     */
    public ErrorSet withErrors(final Multimap<String, String> errors) {
        return new ErrorSet(errors, relations);
    }

    /**
     * Get the bundle's relation bundles.
     *
     * @return A set of relations
     */
    public Multimap<String, ErrorSet> getRelations() {
        return relations;
    }

    /**
     * Set entire set of relations.
     *
     * @param relations A map of relations
     * @return A new error set
     */
    public ErrorSet withRelations(Multimap<String, ErrorSet> relations) {
        return new ErrorSet(errors, relations);
    }

    /**
     * Get a set of relations.
     *
     * @param relation A relation label
     * @return A new error set
     */
    public Collection<ErrorSet> getRelations(String relation) {
        return relations.get(relation);
    }

    /**
     * Set bundles for a particular relation.
     *
     * @param relation A relation label
     * @param others A set of relation error sets
     * @return A new error set
     */
    public ErrorSet withRelations(String relation, List<ErrorSet> others) {
        LinkedListMultimap<String, ErrorSet> tmp = LinkedListMultimap
                .create(relations);
        tmp.putAll(relation, others);
        return new ErrorSet(errors, tmp);
    }

    /**
     * Add a bundle for a particular relation.
     *
     * @param relation A relation label
     * @param other An error set relation
     */
    public ErrorSet withRelation(String relation, ErrorSet other) {
        LinkedListMultimap<String, ErrorSet> tmp = LinkedListMultimap
                .create(relations);
        tmp.put(relation, other);
        return new ErrorSet(errors, tmp);
    }

    /**
     * Serialize a error set to raw data.
     *
     * @return A map of data
     */
    public Map<String, Object> toData() {
        return DataConverter.errorSetToData(this);
    }

    @Override
    public String toString() {
        return toJson();
    }

    /**
     * Serialize a error set to a JSON string.
     *
     * @return json string
     */
    public String toJson() {
        try {
            return DataConverter.errorSetToJson(this);
        } catch (SerializationError e) {
            return "Invalid Errors: " + e.getMessage();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorSet other = (ErrorSet) o;

        return errors.equals(other.errors)
                && unorderedRelations(relations)
                    .equals(unorderedRelations(other.relations));

    }

    @Override
    public int hashCode() {
        int result = errors.hashCode();
        result = 31 * result + relations.hashCode();
        return result;
    }

    /**
     * Is this ErrorSet empty? It will be if there are
     * no errors and none of the relations have errors.
     *
     * @return Whether or not the set is empty.
     */
    public boolean isEmpty() {
        if (!errors.isEmpty())
            return false;
        for (Map.Entry<String, ErrorSet> rel : relations.entries()) {
            if (!rel.getValue().isEmpty())
                return false;
        }
        return true;
    }

    /**
     * Convert the ordered relationship set into an unordered one for comparison.
     * FIXME: Clean up the code and optimise this function.
     */
    private Map<String, LinkedHashMultiset<ErrorSet>> unorderedRelations(final Multimap<String, ErrorSet> rels) {
        Map<String, LinkedHashMultiset<ErrorSet>> map = Maps.newHashMap();
        for (Map.Entry<String, Collection<ErrorSet>> entry : rels.asMap().entrySet()) {
            map.put(entry.getKey(), LinkedHashMultiset.create(entry.getValue()));
        }
        return map;
    }
}
