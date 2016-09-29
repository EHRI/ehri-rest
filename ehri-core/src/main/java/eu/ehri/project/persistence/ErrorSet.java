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

package eu.ehri.project.persistence;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import eu.ehri.project.exceptions.SerializationError;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents a set of validation errors associated with a bundle.
 */
public final class ErrorSet implements NestableData<ErrorSet> {
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

        Builder addError(String key, String error) {
            errors.put(key, error);
            return this;
        }

        Builder addRelation(String relation, ErrorSet errorSet) {
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
     * @param key   The property key in error
     * @param error The string description of the property error
     */
    public static ErrorSet fromError(String key, String error) {
        Multimap<String, String> tmp = ArrayListMultimap.create();
        tmp.put(key, error);
        return new ErrorSet(tmp);
    }

    /**
     * Constructor.
     *
     * @param errors A map of top-level errors
     */
    public ErrorSet(Multimap<String, String> errors) {
        this(errors, ArrayListMultimap.<String, ErrorSet>create());
    }

    /**
     * Constructor.
     *
     * @param errors    A map of top-level errors
     * @param relations A map of relations
     */
    public ErrorSet(Multimap<String, String> errors, Multimap<String, ErrorSet> relations) {
        this.errors = ImmutableListMultimap.copyOf(errors);
        this.relations = ImmutableListMultimap.copyOf(relations);
    }

    /**
     * Get errors for a key.
     *
     * @param key A property key
     * @return A list of errors associated with a property key
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<String> getDataValue(String key) {
        checkNotNull(key);
        return errors.get(key);
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
     * Get the bundle's relation bundles.
     *
     * @return A set of relations
     */
    @Override
    public Multimap<String, ErrorSet> getRelations() {
        return relations;
    }

    /**
     * Add entire set of relations.
     *
     * @param newRelations A map of relations
     * @return A new error set
     */
    @Override
    public ErrorSet withRelations(Multimap<String, ErrorSet> newRelations) {
        Multimap<String, ErrorSet> tmp = ArrayListMultimap.create(relations);
        tmp.putAll(newRelations);
        return new ErrorSet(errors, tmp);
    }

    /**
     * Get a set of relations.
     *
     * @param relation A relation label
     * @return A new error set
     */
    @Override
    public List<ErrorSet> getRelations(String relation) {
        return Lists.newArrayList(relations.get(relation));
    }

    @Override
    public boolean hasRelations(String relation) {
        return relations.containsKey(relation);
    }

    @Override
    public ErrorSet replaceRelations(Multimap<String, ErrorSet> relations) {
        return new ErrorSet(errors, relations);
    }

    /**
     * Set a value in the bundle's data.
     *
     * @param key A property key
     * @param err A description of the error
     * @return A new error set
     */
    @Override
    public ErrorSet withDataValue(String key, Object err) {
        if (err == null) {
            return this;
        } else {
            Multimap<String, String> tmp = ArrayListMultimap.create(errors);
            tmp.put(key, err.toString());
            return new ErrorSet(tmp, relations);
        }
    }

    @Override
    public ErrorSet removeDataValue(String key) {
        Multimap<String, String> tmp = ArrayListMultimap.create(errors);
        tmp.removeAll(key);
        return new ErrorSet(tmp, relations);
    }

    /**
     * Set bundles for a particular relation.
     *
     * @param relation A relation label
     * @param others   A set of relation error sets
     * @return A new error set
     */
    @Override
    public ErrorSet withRelations(String relation, List<ErrorSet> others) {
        Multimap<String, ErrorSet> tmp = ArrayListMultimap
                .create(relations);
        tmp.putAll(relation, others);
        return new ErrorSet(errors, tmp);
    }

    /**
     * Add a bundle for a particular relation.
     *
     * @param relation A relation label
     * @param other    An error set relation
     */
    @Override
    public ErrorSet withRelation(String relation, ErrorSet other) {
        Multimap<String, ErrorSet> tmp = ArrayListMultimap
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
    private Map<String, LinkedHashMultiset<ErrorSet>> unorderedRelations(Multimap<String, ErrorSet> rels) {
        Map<String, LinkedHashMultiset<ErrorSet>> map = Maps.newHashMap();
        for (Map.Entry<String, Collection<ErrorSet>> entry : rels.asMap().entrySet()) {
            map.put(entry.getKey(), LinkedHashMultiset.create(entry.getValue()));
        }
        return map;
    }
}
