package eu.ehri.project.persistance;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.ValidationError;
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
    private static final String MISSING_PROPERTY = "Missing mandatory field";
    private static final String EMPTY_VALUE = "No value given for mandatory field";
    private static final String INVALID_ENTITY = "No EntityType annotation";

    protected final Object id;
    protected final Map<String, Object> data;
    protected final Class<T> cls;
    protected final MultiValueMap relations;

    private MultiValueMap errors = new MultiValueMap();

    /**
     * Constructor.
     * 
     * @param id
     * @param data
     * @param cls
     * @param relations
     */

    public EntityBundle(Object id, final Map<String, Object> data,
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
     * @return
     */
    public EntityBundle<T> addRelation(String relation,
            EntityBundle<? extends VertexFrame> other) {
        MultiValueMap tmp = new MultiValueMap();
        for (Object key : relations.keySet()) {
            tmp.putAll(key, relations.getCollection(key));
        }
        tmp.put(relation, other);
        return new EntityBundle<T>(id, data, cls, tmp);
    }

    /**
     * Set bundles for a particular relation.
     * 
     * @param relation
     * @param others
     * @return
     */
    public EntityBundle<T> setRelations(String relation,
            List<EntityBundle<? extends VertexFrame>> others) {
        MultiValueMap tmp = new MultiValueMap();
        tmp.putAll(relation, others);
        for (Object key : relations.keySet()) {
            if (!key.equals(relation))
                tmp.putAll(key, relations.getCollection(key));
        }
        return new EntityBundle<T>(id, data, cls, tmp);
    }

    /**
     * Get the id of the bundle's graph vertex (or null if it does not yet
     * exist.
     * 
     * @return
     */
    public Object getId() {
        return id;
    }

    /**
     * Determine if the bundle has errors after a validation operation.
     * 
     * @return
     */
    public Boolean hasErrors() {
        return !errors.isEmpty();
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
     * Get any validation errors that exist after a validation operation.
     * 
     * @return
     */
    public MultiValueMap getValidationErrors() {
        return errors;
    }

    /**
     * Get the bundle data.
     * 
     * @return
     */
    public Map<String, Object> getData() {
        return extendData();
    }

    /**
     * Set a value in the bundle's data.
     * 
     * @param key
     * @param value
     * @return
     * @throws ValidationError
     */
    public EntityBundle<T> setDataValue(String key, Object value)
            throws ValidationError {
        // FIXME: Seems like too much work being done here to maintain
        // immutability???
        Map<String, Object> temp = new HashMap<String, Object>(data);
        temp.put(key, value);
        return new EntityBundle<T>(id, temp, cls, relations);
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
    public String getEntityType() {
        EntityType ann = cls.getAnnotation(EntityType.class);
        if (ann == null)
            throw new RuntimeException(String.format(
                    "Programming error! Bad bundle type: %s", cls.getName()));
        return ann.value();
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for updating in the graph.
     * 
     * @throws ValidationError
     */
    public void validateForUpdate() throws ValidationError {
        if (id == null)
            throw new ValidationError(
                    "No identifier given for update operation.");
        validate();
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for insert into the graph.
     * 
     * @throws ValidationError
     */
    public void validateForInsert() throws ValidationError {
        if (id != null)
            throw new ValidationError(
                    String.format(
                            "Identifier is present ('%s') but insert operation specified.",
                            id));
        validate();
    }

    /**
     * Validate the data in the bundle, according to the target class.
     * 
     * @throws ValidationError
     */
    public void validate() throws ValidationError {
        checkFields();
        checkIsA();
        if (hasErrors())
            throw new ValidationError(cls, errors);
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

    @Override
    public String toString() {
        return String.format("<%s: %s>", cls.getName(), data);
    }

    // Helpers

    /**
     * @param data
     * @param cls
     * @param errors
     */
    private void checkFields() {
        for (String key : ClassUtils.getPropertyKeys(cls)) {
            checkField(key);
        }
    }

    /**
     * Check the data holds a given field, accounting for the
     * 
     * @param name
     */
    private void checkField(String name) {
        if (!data.containsKey(name)) {
            errors.put(name, MISSING_PROPERTY);
        } else {
            Object value = data.get(name);
            if (value == null) {
                errors.put(name, EMPTY_VALUE);
            } else if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    errors.put(name, EMPTY_VALUE);
                }
            }
        }
    }

    /**
     * Ensure the returned data always contains an entity type key consistent
     * with the current class.
     * 
     * @return
     */
    private Map<String, Object> extendData() {
        Map<String, Object> ext = new HashMap<String, Object>(data);
        ext.put(EntityType.KEY, getEntityType());
        return ext;
    }

    /**
     * @param data
     * @param cls
     * @param errors
     */
    private void checkIsA() {
        EntityType annotation = cls.getAnnotation(EntityType.class);
        if (annotation == null) {
            errors.put("class",
                    String.format("%s: '%s'", INVALID_ENTITY, cls.getName()));
        }
    }
}
