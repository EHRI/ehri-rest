package eu.ehri.project.persistance;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Class that represents a graph entity and its set of relations.
 * 
 * @author michaelb
 * 
 * @param <T>
 */
public class Bundle {
    private final String id;
    private final EntityClass type;
    private final Map<String, Object> data;
    private final ListMultimap<String,Bundle> relations;

    /**
     * Constructor.
     * 
     * @param id
     * @param cls
     * @param data
     * @param relations
     */
    public Bundle(String id, EntityClass type, final Map<String, Object> data,
            final ListMultimap<String,Bundle> relations) {
        this.id = id;
        this.data = new HashMap<String, Object>(data);
        this.type = type;
        this.relations = relations;
    }

    /**
     * Constructor for bundle without existing id.
     * 
     * @param cls
     * @param data
     * @param relations
     */
    public Bundle(EntityClass type, final Map<String, Object> data,
            final ListMultimap<String,Bundle> relations) {
        this(null, type, data, relations);
    }

    /**
     * Constructor for bundle without existing id or relations.
     * 
     * @param cls
     * @param data
     * @param relations
     */
    public Bundle(EntityClass type, final Map<String, Object> data) {
        this(null, type, data, LinkedListMultimap.<String,Bundle>create());
    }

    /**
     * Add a bundle for a particular relation.
     * 
     * @param relation
     * @param other
     */
    public void addRelation(String relation, Bundle other) {
        relations.put(relation, other);
    }

    /**
     * Set bundles for a particular relation.
     * 
     * @param relation
     * @param others
     * @return
     */
    public void setRelations(String relation, List<Bundle> others) {
        relations.putAll(relation, others);
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
    public ListMultimap<String,Bundle> getRelations() {
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
     * Set a value in the bundle's data.
     * 
     * @param key
     * @param value
     * @throws ValidationError
     */
    public void setDataValue(String key, Object value) {
        data.put(key, value);
    }

    public Bundle setData(final Map<String, Object> data) {
        return new Bundle(id, type, data, relations);
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
