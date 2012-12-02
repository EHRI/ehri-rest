package eu.ehri.project.persistance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;

/**
 * Class that represents a graph entity and its set of relations.
 * 
 * @author michaelb
 * 
 * @param <T>
 */
public class EntityBundle<T extends VertexFrame> {
    protected final String id;
    protected final Map<String, Object> data;
    protected final Class<T> cls;
    protected final MultiValueMap relations;

    /**
     * Constructor.
     * 
     * @param id
     * @param data
     * @param cls
     * @param relations
     */

    public EntityBundle(String id, final Map<String, Object> data,
            Class<T> cls, final MultiValueMap relations) {
        this.id = id;
        this.data = new HashMap<String, Object>(data);
        this.cls = cls;
        this.relations = relations;
    }

    /**
     * Add a bundle for a particular relation.
     * 
     * @param relation
     * @param other
     */
    public void addRelation(String relation,
            EntityBundle<? extends VertexFrame> other) {
        relations.put(relation, other);
    }
    
    /**
     * Set bundles for a particular relation.
     * 
     * @param relation
     * @param others
     * @return
     */
    public void setRelations(String relation,
        List<EntityBundle<? extends VertexFrame>> others) {
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
     * @param id
     */
    public EntityBundle<T> withId(String id) {
        return new EntityBundle<T>(id, data, cls, relations);
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

    public EntityBundle<T> setData(final Map<String, Object> data) {
        return new EntityBundle<T>(id, data, cls, relations);
    }

    /**
     * Get the target class.
     * 
     * @return
     */
    public Class<T> getBundleClass() {
        return cls;
    }

    /**
     * Get the type of entity this bundle represents as per the target class's
     * entity type key.
     * 
     * @return
     */
    public String getType() {
        return ClassUtils.getEntityType(cls);
    }

    /**
     * Return a list of names for mandatory properties, as represented in the
     * graph.
     * 
     * @return
     */
    public List<String> getPropertyKeys() {
        return ClassUtils.getPropertyKeys(cls);
    }

    /**
     * Return a list of names for mandatory properties, as represented in the
     * graph.
     * 
     * @return
     */
    public List<String> getVertexPropertyKeys() {
        List<String> keys = ClassUtils.getPropertyKeys(cls);
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
        return ClassUtils.getUniquePropertyKeys(cls);
    }

    @Override
    public String toString() {
        return String.format("<%s: %s>", cls.getName(), data);
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
            if (!(key.equals(EntityType.ID_KEY) || key.equals(EntityType.TYPE_KEY)) )
                ext.put(key, data.get(key));
        }
        return ext;
    }
}
