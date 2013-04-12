package eu.ehri.project.persistance;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.utils.ClassUtils;
import org.w3c.dom.Document;

/**
 * Class that represents a graph entity and subtree relations.
 * 
 * @author michaelb
 * 
 */
public class Bundle {
    private final String id;
    private final EntityClass type;
    private final ImmutableMap<String, Object> data;
    private final ImmutableListMultimap<String, Bundle> relations;

    /**
     * Serialization constant definitions
     */
    public static final String ID_KEY = "id";
    public static final String REL_KEY = "relationships";
    public static final String DATA_KEY = "data";
    public static final String TYPE_KEY = "type";

    /**
     * Constructor.
     * 
     * @param id
     * @param type
     * @param data
     * @param relations
     */
    public Bundle(String id, EntityClass type, final Map<String, Object> data,
            final ListMultimap<String, Bundle> relations) {
        this.id = id;
        this.type = type;
        this.data = ImmutableMap.copyOf(data);
        this.relations = ImmutableListMultimap.copyOf(relations);
    }

    /**
     * Constructor for bundle without existing id.
     * 
     * @param type
     * @param data
     * @param relations
     */
    public Bundle(EntityClass type, final Map<String, Object> data,
            final ListMultimap<String, Bundle> relations) {
        this(null, type, data, relations);
    }

    /**
     * Constructor for just a type.
     * 
     * @param type
     */
    public Bundle(EntityClass type) {
        this(null, type, Maps.<String, Object> newHashMap(), LinkedListMultimap
                .<String, Bundle> create());
    }

    /**
     * Constructor for bundle without existing id or relations.
     * 
     * @param type
     * @param data
     */
    public Bundle(EntityClass type, final Map<String, Object> data) {
        this(null, type, data, LinkedListMultimap.<String, Bundle> create());
    }

    /**
     * Get the id of the bundle's graph vertex (or null if it does not yet
     * exist.
     * 
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Get a bundle with the given id.
     * 
     * @param id
     */
    public Bundle withId(String id) {
        checkNotNull(id);
        return new Bundle(id, type, data, relations);
    }

    /**
     * Get the type of entity this bundle represents as per the target class's
     * entity type key.
     * 
     * @return
     */
    public EntityClass getType() {
        return type;
    }

    /**
     * Get a data value.
     * 
     * @return
     */
    public Object getDataValue(String key) {
        checkNotNull(key);
        return data.get(key);
    }

    /**
     * Set a value in the bundle's data.
     * 
     * @param key
     * @param value
     * @return
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
     * Remove a value in the bundle's data.
     * 
     * @param key
     * @return
     */
    public Bundle removeDataValue(String key) {
        Map<String, Object> newData = Maps.newHashMap(data);
        newData.remove(key);
        return withData(newData);
    }

    /**
     * Get the bundle data.
     * 
     * @return
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Set the entire data map for this bundle.
     * 
     * @param data
     * @return
     */
    public Bundle withData(final Map<String, Object> data) {
        return new Bundle(id, type, data, relations);
    }

    /**
     * Get the bundle's relation bundles.
     * 
     * @return
     */
    public ListMultimap<String, Bundle> getRelations() {
        return relations;
    }

    /**
     * Set entire set of relations.
     * 
     * @param relations
     * @return
     */
    public Bundle withRelations(ListMultimap<String, Bundle> relations) {
        return new Bundle(id, type, data, relations);
    }

    /**
     * Get a set of relations.
     * 
     * @param relation
     * @return
     */
    public List<Bundle> getRelations(String relation) {
        return relations.get(relation);
    }

    /**
     * Set bundles for a particular relation.
     * 
     * @param relation
     * @param others
     * @return
     */
    public Bundle withRelations(String relation, List<Bundle> others) {
        LinkedListMultimap<String, Bundle> tmp = LinkedListMultimap
                .create(relations);
        tmp.putAll(relation, others);
        return new Bundle(id, type, data, tmp);
    }

    /**
     * Add a bundle for a particular relation.
     * 
     * @param relation
     * @param other
     */
    public Bundle withRelation(String relation, Bundle other) {
        LinkedListMultimap<String, Bundle> tmp = LinkedListMultimap
                .create(relations);
        tmp.put(relation, other);
        return new Bundle(id, type, data, tmp);
    }

    /**
     * Check if this bundle contains the given relation set.
     * 
     * @param relation
     * @return
     */
    public boolean hasRelations(String relation) {
        return relations.containsKey(relation);
    }

    /**
     * Remove a single relation.
     * 
     * @param relation
     * @return
     */
    public Bundle removeRelation(String relation, Bundle item) {
        ListMultimap<String, Bundle> tmp = LinkedListMultimap.create(relations);
        tmp.remove(relation, item);
        return new Bundle(id, type, data, tmp);
    }

    /**
     * Remove a set of relationships.
     * 
     * @param relation
     * @return
     */
    public Bundle removeRelations(String relation) {
        ListMultimap<String, Bundle> tmp = LinkedListMultimap.create(relations);
        tmp.removeAll(relation);
        return new Bundle(id, type, data, tmp);
    }

    /**
     * Get the target class.
     * 
     * @return
     */
    public Class<?> getBundleClass() {
        return type.getEntityClass();
    }

    /**
     * Return a list of names for mandatory properties, as represented in the
     * graph.
     * 
     * @return
     */
    public Iterable<String> getPropertyKeys() {
        return ClassUtils.getPropertyKeys(type.getEntityClass());
    }

    /**
     * Return a list of property keys which must be unique.
     * 
     * @return
     */
    public Iterable<String> getUniquePropertyKeys() {
        return ClassUtils.getUniquePropertyKeys(type.getEntityClass());
    }

    /**
     * Create a bundle from raw data.
     * 
     * @param data
     * @return
     * @throws DeserializationError
     */
    public static Bundle fromData(Object data) throws DeserializationError {
        return DataConverter.dataToBundle(data);
    }

    /**
     * Serialize a bundle to raw data.
     * 
     * @return
     */
    public Map<String, Object> toData() {
        return DataConverter.bundleToData(this);
    }

    /**
     * Create a bundle from a (JSON) string.
     * 
     * @param json
     * @return
     * @throws DeserializationError
     */
    public static Bundle fromString(String json) throws DeserializationError {
        return DataConverter.jsonToBundle(json);
    }

    /**
     * Serialize a bundle to a JSON string.
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
     * @return document
     */
    public Document toXml() {
        return DataConverter.bundleToXml(this);
    }

    /**
     * Serialize to an XML String.
     * @return
     */
    public String toXmlString() {
        return DataConverter.bundleToXmlString(this);
    }
}
