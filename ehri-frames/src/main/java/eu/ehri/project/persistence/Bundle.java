package eu.ehri.project.persistence;

import com.google.common.collect.*;
import com.tinkerpop.blueprints.Direction;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.idgen.IdGenerator;
import eu.ehri.project.models.utils.ClassUtils;
import org.w3c.dom.Document;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents a graph entity and subtree relations.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class Bundle {
    private final boolean temp;
    private final String id;
    private final EntityClass type;
    private final ImmutableMap<String, Object> data;
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
     * Properties that are "managed", i.e. automatically set
     * date/time strings or cache values should begin with a
     * prefix and are ignored Bundle equality calculations.
     */
    public static final String MANAGED_PREFIX = "_";

    public static class Builder {
        private String id = null;
        private final EntityClass type;
        final ListMultimap<String, Bundle> relations = ArrayListMultimap.create();
        final Map<String, Object> data = Maps.newHashMap();
        final Map<String, Object> meta = Maps.newHashMap();

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder(EntityClass cls) {
            type = cls;
        }

        public Builder addRelations(ListMultimap<String, Bundle> r) {
            relations.putAll(r);
            return this;
        }

        public Builder addRelation(String relation, Bundle bundle) {
            relations.put(relation, bundle);
            return this;
        }

        public Builder addData(Map<String,Object> d) {
            data.putAll(d);
            return this;
        }

        public Builder addDataValue(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public Builder addMetaData(Map<String,Object> d) {
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
    private Bundle(String id, EntityClass type, final Map<String, Object> data,
            final ListMultimap<String, Bundle> relations, final Map<String, Object> meta, boolean temp) {
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
    public Bundle(String id, EntityClass type, final Map<String, Object> data,
            final ListMultimap<String, Bundle> relations) {
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
    public Bundle(String id, EntityClass type, final Map<String, Object> data,
            final ListMultimap<String, Bundle> relations, final Map<String, Object> meta) {
        this(id, type, data, relations, meta, false);
    }

    /**
     * Constructor for bundle without existing id.
     *
     * @param type      The bundle's type class
     * @param data      An initial map of data
     * @param relations An initial set of relations
     */
    public Bundle(EntityClass type, final Map<String, Object> data,
            final ListMultimap<String, Bundle> relations) {
        this(null, type, data, relations, Maps.<String, Object>newHashMap());
    }

    /**
     * Constructor for just a type.
     *
     * @param type The bundle's type class
     */
    public Bundle(EntityClass type) {
        this(null, type, Maps.<String, Object>newHashMap(), LinkedListMultimap
                .<String, Bundle>create(), Maps.<String, Object>newHashMap());
    }

    /**
     * Constructor for bundle without existing id or relations.
     *
     * @param type The bundle's type class
     * @param data An initial map of data
     */
    public Bundle(EntityClass type, final Map<String, Object> data) {
        this(null, type, data, LinkedListMultimap.<String, Bundle>create(),
                Maps.<String, Object>newHashMap());
    }

    /**
     * Get the id of the bundle's graph vertex (or null if it does not yet
     * exist.
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
     * Get a data value.
     *
     * @return The data value
     */
    public Object getDataValue(String key) {
        checkNotNull(key);
        return data.get(key);
    }

    /**
     * Set a value in the bundle's data.
     *
     * @param key   The data key
     * @param value The data value
     * @return A new bundle
     */
    public Bundle withDataValue(String key, Object value) {
        if (value == null) {
            return this;
        } else {
            Map<String, Object> newData = Maps.newHashMap(data);
            newData.put(key, value);
            return withData(newData);
        }
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
        return data;
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
    public Bundle withData(final Map<String, Object> data) {
        return new Bundle(id, type, data, relations, meta, temp);
    }

    /**
     * Set the entire meta data map for this bundle.
     *
     * @param meta The full metadata map to set
     * @return The new bundle
     */
    public Bundle withMetaData(final Map<String, Object> meta) {
        return new Bundle(id, type, data, relations, meta, temp);
    }

    /**
     * Get the bundle's relation bundles.
     *
     * @return The full set of relations
     */
    public ListMultimap<String, Bundle> getRelations() {
        return relations;
    }

    /**
     * Get only the bundle's relations which have a dependent
     * relationship.
     * 
     * @return
     */
    public ListMultimap<String,Bundle> getDependentRelations() {
        ListMultimap<String, Bundle> dependentRelations = ArrayListMultimap.create();
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(type.getEntityClass());
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
    public Bundle withRelations(ListMultimap<String, Bundle> relations) {
        return new Bundle(id, type, data, relations, meta, temp);
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
        LinkedListMultimap<String, Bundle> tmp = LinkedListMultimap
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
        LinkedListMultimap<String, Bundle> tmp = LinkedListMultimap
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
        ListMultimap<String, Bundle> tmp = LinkedListMultimap.create(relations);
        tmp.remove(relation, item);
        return new Bundle(id, type, data, tmp, meta, temp);
    }

    /**
     * Merge this bundle's data with that of another. Note: currently
     * relation data is not merged.
     *
     * @param otherBundle Another bundle
     * @return A bundle with data merged
     */
    public Bundle mergeDataWith(Bundle otherBundle) {
        Map<String, Object> mergeData = Maps.newHashMap(getData());
        mergeData.putAll(otherBundle.getData());
        return withData(mergeData);
    }

    /**
     * Get the target class.
     *
     * @return The bundle's type class
     */
    public Class<?> getBundleClass() {
        return type.getEntityClass();
    }

    /**
     * Return a list of names for mandatory properties, as represented in the
     * graph.
     *
     * @return A list of property keys for the bundle's type
     */
    public Iterable<String> getPropertyKeys() {
        return ClassUtils.getPropertyKeys(type.getEntityClass());
    }

    /**
     * Return a list of property keys which must be unique.
     *
     * @return A list of unique property keys for the bundle's type
     */
    public Iterable<String> getUniquePropertyKeys() {
        return ClassUtils.getUniquePropertyKeys(type.getEntityClass());
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
     * Serialize a bundle to a JSON string.
     *
     * @return document An XML document
     */
    public Document toXml() {
        return DataConverter.bundleToXml(this);
    }

    /**
     * Serialize to an XML String.
     *
     * @return An XML string
     */
    public String toXmlString() {
        return DataConverter.bundleToXmlString(this);
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
     * Generate missing IDs for the subtree.
     *
     * @param scopes A set of parent scopes.
     * @return A new bundle
     */
    public Bundle generateIds(final List<String> scopes) {
        boolean isTemp = id == null;
        IdGenerator idGen = getType().getIdgen();
        String newId = isTemp ? idGen.generateId(scopes, this) : id;
        ListMultimap<String, Bundle> idRels = LinkedListMultimap.create();
        List<String> nextScopes = Lists.newArrayList(scopes);
        nextScopes.add(idGen.getIdBase(this));
        for (Map.Entry<String, Bundle> entry : relations.entries()) {
            idRels.put(entry.getKey(), entry.getValue().generateIds(nextScopes));
        }
        return new Bundle(newId, type, data, idRels, meta, isTemp);
    }

    /**
     * Generate missing IDs for the subtree.
     *
     * @param scope A permission scope.
     * @return A new bundle
     */
    public Bundle generateIds(final PermissionScope scope) {
        return generateIds(getScopeIds(scope));
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
     * Return an immutable copy of the given data map with nulls removed.
     */
    private ImmutableMap<String, Object> filterData(Map<String, Object> data) {
        Map<String, Object> filtered = Maps.newHashMap();
        for (Map.Entry<? extends String, Object> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return ImmutableMap.copyOf(filtered);
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
    private Map<String, LinkedHashMultiset<Bundle>> unorderedRelations(final ListMultimap<String, Bundle> rels) {
        Map<String, LinkedHashMultiset<Bundle>> map = Maps.newHashMap();
        for (Map.Entry<String, Collection<Bundle>> entry : rels.asMap().entrySet()) {
            map.put(entry.getKey(), LinkedHashMultiset.create(entry.getValue()));
        }
        return map;
    }

    private List<String> getScopeIds(PermissionScope scope) {
        if (SystemScope.INSTANCE.equals(scope)) {
            return Lists.newArrayList();
        } else {
            LinkedList<String> scopeIds = Lists.newLinkedList();
            if (scope != null) {
                for (PermissionScope s : scope.getPermissionScopes())
                    scopeIds.addFirst(s.getIdentifier());
                scopeIds.add(scope.getIdentifier());
            }
            return scopeIds;
        }
    }
}
