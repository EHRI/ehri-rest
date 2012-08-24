package eu.ehri.project.persistance;

import java.util.HashMap;
import java.util.Map;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.ValidationError;

public class BundleFactory <T extends VertexFrame> {
    
    public EntityBundle<T> buildBundle(Map<String, Object> data, Class<T> cls) throws ValidationError {
        // Take a Frames interface and some data, and check that
        // all the Property annotations are fulfilled.               
        return new EntityBundle<T>(data, cls);
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
