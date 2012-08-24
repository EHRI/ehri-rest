package eu.ehri.project.persistance;

import java.util.Map;

import eu.ehri.project.models.annotations.EntityType;

public class EntityBundle <T> {
    private Map<String,Object> data;
    private Class<T> cls;
    protected EntityBundle(Map<String,Object> data, Class<T> cls) {
        this.data = data;
        this.cls = cls;
    }
    
    public Map<String,Object> getData() {
        return this.data;
    }
    
    public Class<T> getBundleClass() {
        return cls;
    }
    
    public String getEntityType() {
        return cls.getAnnotation(EntityType.class).value();
    }
}
