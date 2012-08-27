package eu.ehri.project.persistance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.annotations.EntityType;

import org.apache.commons.collections.map.MultiValueMap;


public class EntityBundle <T extends VertexFrame> implements DataBundle<T> {
    private static final String GET = "get";
    private static final String MISSING_PROPERTY = "Missing mandatory field";
    private static final String EMPTY_VALUE = "No value given for mandatory field";
    private static final String INVALID_ENTITY = "No EntityType annotation";

    protected final Map<String,Object> data;
    protected final Class<T> cls;
    protected final MultiValueMap saveWith;
    
    private MultiValueMap errors = new MultiValueMap();
    
    protected EntityBundle(final Map<String,Object> data, Class<T> cls, final MultiValueMap saveWith) {
        this.data = new HashMap<String,Object>(data);
        this.cls = cls;
        this.saveWith = saveWith;
    }
    
    public EntityBundle<T> saveWith(String relation, EntityBundle<? extends VertexFrame> other) {
        MultiValueMap tmp = new MultiValueMap();
        for (Object key : saveWith.keySet()) {
            tmp.put(key, saveWith.get(key));
        }
        tmp.put(relation, other);
        return new EntityBundle<T>(data, cls, tmp);
    }
    
    private Map<String,Object> extendData() {
        Map<String,Object> ext = new HashMap<String,Object>(data);
        ext.put(EntityTypes.KEY, getEntityType());
        return ext;
    }
    
    public Boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public MultiValueMap getSaveWith() {
        return saveWith;
    }

    public MultiValueMap getValidationErrors() {
        return errors;
    }
    
    public Map<String,Object> getData() throws ValidationError {
        return extendData();
    }
    
    public DataBundle<T> setDataValue(String key, Object value) throws ValidationError {
        // FIXME: Seems like too much work being done here to maintain immutability???
        Map<String,Object> temp = new HashMap<String,Object>(data);
        temp.put(key, value);
        return new EntityBundle<T>(temp, cls, saveWith);
    }
    
    public EntityBundle<T> setData(final Map<String, Object> data) {
        return new EntityBundle<T>(data, cls, saveWith);
    }
    
    public Class<T> getBundleClass() {
        return cls;
    }
    
    public String getEntityType() {
        return cls.getAnnotation(EntityType.class).value();
    }
    
    public void validate() throws ValidationError {
        checkFields();
        checkIsA();
        if (hasErrors())
            throw new ValidationError(cls, errors);
    }

    /**
     * @param data
     * @param cls
     * @param errors
     */
    private void checkFields() {
        for (Method method : cls.getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation instanceof Property && method.getName().startsWith(GET)) {
                    checkField(((Property) annotation).value(), method);
                }
            }
        }
    }
    
    private void checkField(String name, Method method) {
        if (!data.containsKey(name)) {
            errors.put(name, MISSING_PROPERTY);
        } else {
            Object value = data.get(name);
            if (value == null) {
                errors.put(name, EMPTY_VALUE);
            }
        }        
    }

    /**
     * @param data 
     * @param cls
     * @param errors
     */
    private void checkIsA() {
        EntityType annotation = cls.getAnnotation(EntityType.class);
        if (annotation == null) {
            errors.put("class", String.format("%s: '%s'", INVALID_ENTITY, cls.getName()));
        }
    }
    
}
