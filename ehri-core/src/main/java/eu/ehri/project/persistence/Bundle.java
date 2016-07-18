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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Direction;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.models.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents a graph entity and subtree relations
 * prior to being materialised as vertices and edges.
 * <p>
 * Note: unlike a vertex, a bundle can contain null values
 * in its data map, though these values will not be externally
 * visible. Null values <i>are</i> however used in merge operations,
 * where the secondary bundle's null data values will indicate that
 * the key/value should be removed from the primary bundle's data.
 */
public final class Bundle {

    private static final Logger logger = LoggerFactory.getLogger(Bundle.class);

    private final boolean temp;
    private final String id;
    private final EntityClass type;
    private final Map<String, Object> data;
    private final ImmutableMap<String, Object> meta;
    private final ImmutableListMultimap<String, Bundle> relations;

    /**
     * Serialization constant definitions
     */
    public static final String ID_KEY = "id";
    public static final String REL_KEY = "relationships";
    public static final String DATA_KEY = "data";
    public static final String TYPE_KEY = "type";
    public static final String META_KEY = "meta";

    /**
     * Filter predicate function interface.
     */
    public interface Filter {
        /**
         * Filter (remove) items in a Bundle tree that
         * match this predicate.
         *
         * @param bundle The bundle
         * @return Whether to remove the item
         */
        boolean remove(String relationLabel, Bundle bundle);
    }

    /**
     * Properties that are "managed", i.e. automatically set
     * date/time strings or cache values should begin with a
     * prefix and are ignored Bundle equality calculations.
     */
    public static final String MANAGED_PREFIX = "_";

    public static class Builder {
        private String id;
        private final EntityClass type;
        final Multimap<String, Bundle> relations = ArrayListMultimap.create();
        final Map<String, Object> data = Maps.newHashMap();
        final Map<String, Object> meta = Maps.newHashMap();

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        private Builder(EntityClass cls) {
            type = cls;
        }

        public static Builder withClass(EntityClass cls) {
            return new Builder(cls);
        }

        public static Builder from(Bundle bundle) {
            return withClass(bundle.getType())
                    .setId(bundle.getId())
                    .addMetaData(bundle.getMetaData())
                    .addData(bundle.getData())
                    .addRelations(bundle.getRelations());
        }

        public Builder addRelations(Multimap<String, Bundle> r) {
            relations.putAll(r);
            return this;
        }

        public Builder addRelation(String relation, Bundle bundle) {
            relations.put(relation, bundle);
            return this;
        }

        public Builder addData(Map<String, Object> d) {
            data.putAll(d);
            return this;
        }

        public Builder addDataValue(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public Builder addMetaData(Map<String, Object> d) {
            meta.putAll(d);
            return this;
        }

        public Builder addMetaDataValue(String key, Object value) {
            meta.put(key, value);
            return this;
        }

        public Bundle build() {
            return new Bundle(id, type, data, relations, meta);
        }
    }

    /**
     * Constructor.
     *
     * @param id        The bundle's id
     * @param type      The bundle's type class
     * @param data      An initial map of data
     * @param relations An initial set of relations
     * @param meta      An initial map of metadata
     * @param temp      A marker to indicate the ID has been generated
     */
    private Bundle(String id, EntityClass type, Map<String, Object> data,
            Multimap<String, Bundle> relations, Map<String, Object> meta, boolean temp) {
        this.id = id;
        this.type = type;
        this.data = filterData(data);
        this.meta = ImmutableMap.copyOf(meta);
        this.relations = ImmutableListMultimap.copyOf(relations);
        this.temp = temp;
    }

    /**
     * Constructor for bundle without existing id.
     *
     * @param id        The bundle's id
     * @param type      The bundle's type class
     * @param data      An initial map of data
     * @param relations An initial set of relations
     */
    public Bundle(String id, EntityClass type, Map<String, Object> data,
            Multimap<String, Bundle> relations) {
        this(id, type, data, relations, Maps.<String, Object>newHashMap());
    }

    /**
     * Constructor.
     *
     * @param id        The bundle's id
     * @param type      The bundle's type class
     * @param data      An initial map of data
     * @param relations An initial set of relations
     * @param meta      An initial map of metadata
     */
    public Bundle(String id, EntityClass type, Map<String, Object> data,
            Multimap<String, Bundle> relations, Map<String, Object> meta) {
        this(id, type, data, relations, meta, false);
    }

    /**
     * Constructor for bundle without existing id.
     *
     * @param type      The bundle's type class
     * @param data      An initial map of data
     * @param relations An initial set of relations
     */
    public Bundle(EntityClass type, Map<String, Object> data,
            Multimap<String, Bundle> relations) {
        this(null, type, data, relations, Maps.<String, Object>newHashMap());
    }

    /**
     * Constructor for just a type.
     *
     * @param type The bundle's type class
     */
    public Bundle(EntityClass type) {
        this(null, type, Maps.<String, Object>newHashMap(), ArrayListMultimap
                .<String, Bundle>create(), Maps.<String, Object>newHashMap());
    }

    /**
     * Constructor for bundle without existing id or relations.
     *
     * @param type The bundle's type class
     * @param data An initial map of data
     */
    public Bundle(EntityClass type, Map<String, Object> data) {
        this(null, type, data, ArrayListMultimap.<String, Bundle>create(),
                Maps.<String, Object>newHashMap());
    }

    /**
     * Get the id of the bundle's graph vertex (or null if it does not yet
     * exist).
     *
     * @return The bundle's id
     */
    public String getId() {
        return id;
    }

    /**
     * Get a bundle with the given id.
     *
     * @param id The bundle's id
     */
    public Bundle withId(String id) {
        checkNotNull(id);
        return new Bundle(id, type, data, relations, meta, temp);
    }

    /**
     * Get the type of entity this bundle represents as per the target class's
     * entity type key.
     *
     * @return The bundle's type
     */
    public EntityClass getType() {
        return type;
    }

    /**
     * Get a data value by its key.
     *
     * @return The data value, or null if there is no data for this key
     * @throws ClassCastException if the fetched data does not match the
     *                            type requested
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataValue(String key) throws ClassCastException {
        checkNotNull(key);
        return (T) data.get(key);
    }

    /**
     * Set a value in the bundle's data. If value is null,
     * this Bundle is returned.
     *
     * @param key   The data key
     * @param value The data value
     * @return A new bundle with value as key
     */
    public Bundle withDataValue(String key, Object value) {
        Map<String, Object> newData = Maps.newHashMap(data);
        newData.put(key, value);
        return withData(newData);
    }

    /**
     * Set a value in the bundle's meta data.
     *
     * @param key   The metadata key
     * @param value The metadata value
     * @return A new bundle
     */
    public Bundle withMetaDataValue(String key, Object value) {
        if (value == null) {
            return this;
        } else {
            Map<String, Object> newData = Maps.newHashMap(meta);
            newData.put(key, value);
            return withMetaData(newData);
        }
    }

    /**
     * Remove a value in the bundle's data.
     *
     * @param key The data key to remove
     * @return A new bundle
     */
    public Bundle removeDataValue(String key) {
        Map<String, Object> newData = Maps.newHashMap(data);
        newData.remove(key);
        return withData(newData);
    }

    /**
     * Get the bundle data.
     *
     * @return The full data map
     */
    public Map<String, Object> getData() {
        return ImmutableMap.copyOf(Maps.filterValues(data, value -> value != null));
    }

    /**
     * Get the bundle metadata
     *
     * @return The full metadata map
     */
    public Map<String, Object> getMetaData() {
        return meta;
    }


    /**
     * Check if this bundle has associated metadata.
     *
     * @return Whether metadata is present
     */
    public boolean hasMetaData() {
        return !meta.isEmpty();
    }

    /**
     * Set the entire data map for this bundle.
     *
     * @param data The full data map to set
     * @return The new bundle
     */
    public Bundle withData(Map<String, Object> data) {
        return new Bundle(id, type, data, relations, meta, temp);
    }

    /**
     * Set the entire meta data map for this bundle.
     *
     * @param meta The full metadata map to set
     * @return The new bundle
     */
    public Bundle withMetaData(Map<String, Object> meta) {
        return new Bundle(id, type, data, relations, meta, temp);
    }

    /**
     * Get the bundle's relation bundles.
     *
     * @return The full set of relations
     */
    public Multimap<String, Bundle> getRelations() {
        return relations;
    }

    /**
     * Get only the bundle's relations which have a dependent
     * relationship.
     *
     * @return A multimap of dependent relations.
     */
    public Multimap<String, Bundle> getDependentRelations() {
        Multimap<String, Bundle> dependentRelations = ArrayListMultimap.create();
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(type.getJavaClass());
        for (String relation : relations.keySet()) {
            if (dependents.containsKey(relation)) {
                for (Bundle child : relations.get(relation)) {
                    dependentRelations.put(relation, child);
                }
            }
        }
        return dependentRelations;
    }

    /**
     * Set entire set of relations.
     *
     * @param relations A full set of relations
     * @return The new bundle
     */
    public Bundle replaceRelations(Multimap<String, Bundle> relations) {
        return new Bundle(id, type, data, relations, meta, temp);
    }

    /**
     * Add a map of addition relationships.
     *
     * @param others Additional relationship map
     * @return The new bundle
     */
    public Bundle withRelations(Multimap<String, Bundle> others) {
        Multimap<String, Bundle> tmp = ArrayListMultimap
                .create(relations);
        tmp.putAll(others);
        return new Bundle(id, type, data, tmp, meta, temp);
    }

    /**
     * Get a set of relations.
     *
     * @param relation A relationship key
     * @return A given set of relations
     */
    public List<Bundle> getRelations(String relation) {
        return relations.get(relation);
    }

    /**
     * Set bundles for a particular relation.
     *
     * @param relation A relationship key
     * @param others   A set of relations for the given key
     * @return A new bundle
     */
    public Bundle withRelations(String relation, List<Bundle> others) {
        Multimap<String, Bundle> tmp = ArrayListMultimap
                .create(relations);
        tmp.putAll(relation, others);
        return new Bundle(id, type, data, tmp, meta, temp);
    }

    /**
     * Add a bundle for a particular relation.
     *
     * @param relation A relationship key
     * @param other    A related bundle
     * @return A new bundle
     */
    public Bundle withRelation(String relation, Bundle other) {
        Multimap<String, Bundle> tmp = ArrayListMultimap
                .create(relations);
        tmp.put(relation, other);
        return new Bundle(id, type, data, tmp, meta, temp);
    }

    /**
     * Check if this bundle contains the given relation set.
     *
     * @param relation A relationship key
     * @return Whether this bundle has relations for the given key
     */
    public boolean hasRelations(String relation) {
        return relations.containsKey(relation);
    }

    /**
     * Remove a single relation.
     *
     * @param relation A relationship key
     * @param item     The item to remove
     * @return A new bundle
     */
    public Bundle removeRelation(String relation, Bundle item) {
        Multimap<String, Bundle> tmp = ArrayListMultimap.create(relations);
        tmp.remove(relation, item);
        return new Bundle(id, type, data, tmp, meta, temp);
    }

    /**
     * Merge this bundle's data with that of another. Relation data is merged when
     * corresponding related items exist in the tree, but new related items are not
     * added.
     *
     * @param otherBundle Another bundle
     * @return A bundle with data merged
     */
    public Bundle mergeDataWith(final Bundle otherBundle) {
        Map<String, Object> mergeData = Maps.newHashMap(getData());

        // This merges the data maps so that keys with null values in the
        // second bundle are removed from the current one's data
        logger.trace("Merging data: {}", otherBundle.data);
        for (Map.Entry<String, Object> entry : otherBundle.data.entrySet()) {
            if (entry.getValue() != null) {
                mergeData.put(entry.getKey(), entry.getValue());
            } else {
                logger.trace("Unset key in merge: {}", entry.getKey());
                mergeData.remove(entry.getKey());
            }
        }
        final Builder builder = Builder.withClass(getType()).setId(getId()).addMetaData(meta)
                .addData(mergeData);

        // This is a slightly gnarly algorithm as written.
        // We want to merge two relationship trees
        for (Map.Entry<String, Collection<Bundle>> entry : otherBundle.getRelations().asMap().entrySet()) {
            String relName = entry.getKey();
            if (relations.containsKey(relName)) {
                List<Bundle> relations = getRelations(relName);
                Collection<Bundle> otherRelations = entry.getValue();
                Set<Bundle> updated = Sets.newHashSet();
                for (final Bundle otherRel : otherRelations) {
                    Optional<Bundle> toUpdate = Iterables.tryFind(relations,
                            bundle -> bundle.getId() != null && bundle.getId().equals(otherRel.getId()));
                    if (toUpdate.isPresent()) {
                        Bundle up = toUpdate.get();
                        updated.add(up);
                        builder.addRelation(relName, up.mergeDataWith(otherRel));
                    } else {
                        logger.warn("Ignoring nested bundle in PATCH update: {}", otherRel);
                    }
                }
                for (Bundle bundle : relations) {
                    if (!updated.contains(bundle)) {
                        builder.addRelation(relName, bundle);
                    }
                }
            }
        }

        for (Map.Entry<String, Bundle> entry : relations.entries()) {
            if (!otherBundle.hasRelations(entry.getKey())) {
                builder.addRelation(entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }

    /**
     * Filter relations, removing items that *match* the given
     * filter function.
     *
     * @param filter A Filter function instance
     * @return A bundle with relations matching the
     * given predicate function removed.
     */
    public Bundle filterRelations(Filter filter) {
        Builder builder = new Builder(type)
                .addData(data)
                .addMetaData(meta)
                .setId(id);
        for (Map.Entry<String, Bundle> rel : relations.entries()) {
            if (!filter.remove(rel.getKey(), rel.getValue())) {
                builder.addRelation(rel.getKey(), rel.getValue()
                        .filterRelations(filter));
            }
        }
        return builder.build();
    }

    /**
     * Get the target class.
     *
     * @return The bundle's type class
     */
    public Class<?> getBundleJavaClass() {
        return type.getJavaClass();
    }

    /**
     * Return a list of names for mandatory properties, as represented in the
     * graph.
     *
     * @return A list of property keys for the bundle's type
     */
    public Collection<String> getPropertyKeys() {
        return ClassUtils.getPropertyKeys(type.getJavaClass());
    }

    /**
     * Return a list of property keys which must be unique.
     *
     * @return A list of unique property keys for the bundle's type
     */
    public Collection<String> getUniquePropertyKeys() {
        return ClassUtils.getUniquePropertyKeys(type.getJavaClass());
    }

    /**
     * Create a bundle from raw data.
     *
     * @param data A raw data object
     * @return A bundle
     * @throws DeserializationError
     */
    public static Bundle fromData(Object data) throws DeserializationError {
        return DataConverter.dataToBundle(data);
    }

    /**
     * Serialize a bundle to raw data.
     *
     * @return A raw data object
     */
    public Map<String, Object> toData() {
        return DataConverter.bundleToData(this);
    }

    /**
     * Create a bundle from a (JSON) string.
     *
     * @param json A JSON representation
     * @return A bundle
     * @throws DeserializationError
     */
    public static Bundle fromString(String json) throws DeserializationError {
        return DataConverter.jsonToBundle(json);
    }

    /**
     * Create a bundle from a stream containing JSON data..
     *
     * @param stream A JSON stream
     * @return A bundle
     * @throws DeserializationError
     */
    public static Bundle fromStream(InputStream stream) throws DeserializationError {
        return DataConverter.streamToBundle(stream);
    }

    /**
     * Write a bundle to a JSON stream.
     *
     * @param bundle the bundle
     * @param stream the output stream
     * @throws SerializationError
     */
    public static void toStream(Bundle bundle, OutputStream stream) throws SerializationError {
        DataConverter.bundleToStream(bundle, stream);
    }

    public static CloseableIterable<Bundle> bundleStream(InputStream inputStream) throws DeserializationError {
        return DataConverter.bundleStream(inputStream);
    }

    @Override
    public String toString() {
        return "<" + getType() + ": '" + (id == null ? "?" : id) + "'> (" + getData() + " + Rels: " + relations + ")";
    }

    /**
     * Serialize a bundle to a JSON string.
     *
     * @return json string
     */
    public String toJson() {
        try {
            return DataConverter.bundleToJson(this);
        } catch (SerializationError e) {
            return "Invalid Bundle: " + e.getMessage();
        }
    }

    /**
     * Check if this bundle as a generated ID.
     *
     * @return True if the ID has been synthesised.
     */
    public boolean hasGeneratedId() {
        return temp;
    }

    /**
     * The depth of this bundle tree, i.e. the number
     * of levels of relationships beneath this one.
     *
     * @return the number of levels
     */
    public int depth() {
        int depth = 0;
        for (Bundle rel : relations.values()) {
            depth = Math.max(depth, 1 + rel.depth());
        }
        return depth;
    }

    /**
     * Generate missing IDs for the subtree.
     *
     * @param scopes A set of parent scopes.
     * @return A new bundle
     */
    public Bundle generateIds(Collection<String> scopes) {
        boolean isTemp = id == null;
        IdGenerator idGen = getType().getIdGen();
        String newId = isTemp ? idGen.generateId(scopes, this) : id;
        Multimap<String, Bundle> idRels = ArrayListMultimap.create();
        List<String> nextScopes = Lists.newArrayList(scopes);
        nextScopes.add(idGen.getIdBase(this));
        for (Map.Entry<String, Bundle> entry : relations.entries()) {
            idRels.put(entry.getKey(), entry.getValue().generateIds(nextScopes));
        }
        return new Bundle(newId, type, data, idRels, meta, isTemp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bundle bundle = (Bundle) o;

        return type == bundle.type
                && unmanagedData(data).equals(unmanagedData(bundle.data))
                && unorderedRelations(relations).equals(unorderedRelations(bundle.relations));
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + unmanagedData(data).hashCode();
        result = 31 * result + unorderedRelations(relations).hashCode();
        return result;
    }

    // Helpers...

    /**
     * Return a copy of the given data map with enums converted to string values.
     */
    private Map<String, Object> filterData(Map<String, Object> data) {
        Map<String, Object> filtered = Maps.newHashMap();
        for (Map.Entry<? extends String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Enum<?>) {
                value = ((Enum) value).name();
            }
            filtered.put(entry.getKey(), value);
        }
        return filtered;
    }

    /**
     * Return a set of data with 'managed' items (prefixed by a particular
     * key) removed.
     */
    private Map<String, Object> unmanagedData(Map<String, Object> in) {
        Map<String, Object> filtered = Maps.newHashMap();
        for (Map.Entry<? extends String, Object> entry : in.entrySet()) {
            if (!entry.getKey().startsWith(MANAGED_PREFIX)
                    && entry.getValue() != null) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    /**
     * Convert the ordered relationship set into an unordered one for comparison.
     */
    private Map<String, LinkedHashMultiset<Bundle>> unorderedRelations(Multimap<String, Bundle> rels) {
        Map<String, LinkedHashMultiset<Bundle>> map = Maps.newHashMap();
        for (Map.Entry<String, Collection<Bundle>> entry : rels.asMap().entrySet()) {
            map.put(entry.getKey(), LinkedHashMultiset.create(entry.getValue()));
        }
        return map;
    }
}
