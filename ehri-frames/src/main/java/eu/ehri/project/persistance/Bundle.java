package eu.ehri.project.persistance;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Class that represents a graph entity and its set of relations.
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
     * Constructor.
     * 
     * @param id
     * @param cls
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
     * @param cls
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
     * @param cls
     * @param data
     * @param relations
     */
    public Bundle(EntityClass type) {
        this(null, type, Maps.<String,Object>newHashMap(), LinkedListMultimap
                .<String, Bundle> create());
    }

    /**
     * Constructor for bundle without existing id or relations.
     * 
     * @param cls
     * @param data
     * @param relations
     */
    public Bundle(EntityClass type, final Map<String, Object> data) {
        this(null, type, data, LinkedListMultimap.<String, Bundle> create());
    }

    /**
     * Add a bundle for a particular relation.
     * 
     * @param relation
     * @param other
     */
    public Bundle withRelation(String relation, Bundle other) {
        LinkedListMultimap<String, Bundle> tmp = LinkedListMultimap.create(relations);
        tmp.put(relation, other);
        return new Bundle(id, type, data, tmp);
    }

    /**
     * Set bundles for a particular relation.
     * 
     * @param relation
     * @param others
     * @return
     */
    public Bundle withRelations(String relation, List<Bundle> others) {
        LinkedListMultimap<String, Bundle> tmp = LinkedListMultimap.create(relations);
        tmp.putAll(relation, others);
        return new Bundle(id, type, data, tmp);
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
     * Get the bundle's relation bundles.
     * 
     * @return
     */
    public ListMultimap<String, Bundle> getRelations() {
        return relations;
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
     * @throws ValidationError
     */
    public Bundle withDataValue(String key, Object value) {
        Map<String, Object> newData = Maps.newHashMap(data);
        newData.put(key, value);
        return withData(newData);
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
     * Get a set of relations.
     * 
     * @param relation
     * @return
     */
    public List<Bundle> getRelations(String relation) {
        return relations.get(relation);
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
     * Get the type of entity this bundle represents as per the target class's
     * entity type key.
     * 
     * @return
     */
    public EntityClass getType() {
        return type;
    }

    /**
     * Return a list of names for mandatory properties, as represented in the
     * graph.
     * 
     * @return
     */
    public List<String> getPropertyKeys() {
        return ClassUtils.getPropertyKeys(type.getEntityClass());
    }

    /**
     * Return a list of property keys which must be unique.
     * 
     * @return
     */
    public List<String> getUniquePropertyKeys() {
        return ClassUtils.getUniquePropertyKeys(type.getEntityClass());
    }

    @Override
    public String toString() {
        return String.format("<%s: %s>", type.getName(), data);
    }
}
