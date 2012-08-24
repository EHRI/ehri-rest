package eu.ehri.project.persistance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.ValidationError;

import eu.ehri.project.models.EntityTypes;
import eu.ehri.project.models.annotations.EntityType;

public class BundleFactory <T extends VertexFrame> {
    private static final String GET = "get";
    private static final String MISSING_PROPERTY = "Missing property";
    private static final String INVALID_ENTITY = "No EntityType annotation";
    
    public EntityBundle<T> buildBundle(Map<String, Object> data, Class<T> cls) throws ValidationError {
        // Take a Frames interface and some data, and check that
        // all the Property annotations are fulfilled.
        
        Map<String,String> errors = new HashMap<String,String>();
        Map<String,Object> bundleData = new HashMap<String,Object>(data);
        
        checkIsA(bundleData, cls, errors);        
        checkFields(bundleData, cls, errors);
        
        if (!errors.isEmpty())     
            throw new ValidationError(cls, errors);        
        return new EntityBundle<T>(bundleData, cls);
    }

    /**
     * @param data
     * @param cls
     * @param errors
     */
    private void checkFields(Map<String, Object> data, Class<T> cls,
            Map<String, String> errors) {
        for (Method method : cls.getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation instanceof Property) {
                    if (method.getName().startsWith(GET)) {
                        String name = ((Property) annotation).value();
                        if (!data.containsKey(name)) {
                            errors.put(MISSING_PROPERTY, name);
                        } else {
                            // TODO: Work out how to verify that the data value
                            // is the same type as the getter expects. Perhaps we
                            // Only check this is there's a setter?
                        }
                    }
                }
            }
        }
    }

    /**
     * @param data 
     * @param cls
     * @param errors
     */
    private void checkIsA(Map<String, Object> data, Class<T> cls, Map<String, String> errors) {
        EntityType annotation = cls.getAnnotation(EntityType.class);
        if (annotation == null) {
            errors.put("class", String.format("%s: '%s'", INVALID_ENTITY, cls.getName()));
        } else {
            data.put(EntityTypes.KEY, annotation.value());
        }
    }
    
    @SuppressWarnings("unchecked")
    public EntityBundle<T> fromFramedVertext(T frame) throws ValidationError {
        Vertex vertex = frame.asVertex();
        Map<String,Object> data = new HashMap<String, Object>();
        for (String key: vertex.getPropertyKeys()) {
            data.put(key, vertex.getProperty(key));
        }
        // FIXME: WTF? That we have to run through this rigmarole to get
        // the `T` class from a given FramedVertex instance is a sign that
        // we're doing things very wrong!!!
        return buildBundle(data, (Class<T>) frame.getClass().getInterfaces()[0]);        
    }
}
