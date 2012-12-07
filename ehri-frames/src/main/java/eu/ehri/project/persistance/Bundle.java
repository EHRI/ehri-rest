package eu.ehri.project.persistance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;

import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Class that represents a graph entity and its set of relations.
 * 
 * @author michaelb
 * 
 * @param <T>
 */
public class Bundle {
    protected final String id;
    protected final EntityClass type;
    protected final Map<String, Object> data;
    protected final MultiValueMap relations;

    /**
     * Constructor.
     * 
     * @param id
     * @param cls
     * @param data
     * @param relations
     */
    public Bundle(String id, EntityClass type, final Map<String, Object> data,
            final MultiValueMap relations) {
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
            final MultiValueMap relations) {
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
        this(null, type, data, new MultiValueMap());
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
        return new Bundle(id, type, data, relations);
    }

    /**
     * Get the bundle's relation bundles.
     * 
     * @return
     */
    public MultiValueMap getRelations() {
        return relations;
    }

    /**
     * Get the bundle data.
     * 
     * @return
     */
    public Map<String, Object> getData() {
        return filterData();
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
     * Return a list of names for mandatory properties, as represented in the
     * graph.
     * 
     * @return
     */
    public List<String> getVertexPropertyKeys() {
        List<String> keys = ClassUtils.getPropertyKeys(type.getEntityClass());
        keys.add(EntityType.ID_KEY);
        keys.add(EntityType.TYPE_KEY);
        return keys;
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

    // Helpers

    /**
     * Ensure the returned data always contains an entity type key consistent
     * with the current class.
     * 
     * @return
     */
    private Map<String, Object> filterData() {
        Map<String, Object> ext = new HashMap<String, Object>();
        for (String key : data.keySet()) {
            if (!(key.equals(EntityType.ID_KEY) || key
                    .equals(EntityType.TYPE_KEY)))
                ext.put(key, data.get(key));
        }
        return ext;
    }
}
