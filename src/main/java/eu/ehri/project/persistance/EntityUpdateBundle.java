package eu.ehri.project.persistance;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;

import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.ValidationError;

public class EntityUpdateBundle<T extends VertexFrame> extends EntityBundle<T> {
    private long id = -1;

    protected EntityUpdateBundle(long id, Map<String, Object> data,
            Class<T> cls, MultiValueMap saveWith) {
        super(data, cls, saveWith);
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public EntityUpdateBundle<T> setDataValue(String key, Object value)
            throws ValidationError {
        // FIXME: Seems like too much work being done here to maintain
        // immutability???
        Map<String, Object> temp = new HashMap<String, Object>(data);
        temp.put(key, value);
        return new EntityUpdateBundle<T>(id, temp, cls, saveWith);
    }

    @Override
    public EntityUpdateBundle<T> setData(final Map<String, Object> data) {
        return new EntityUpdateBundle<T>(id, data, cls, saveWith);
    }

    public EntityUpdateBundle<T> saveWith(String relation,
            EntityUpdateBundle<T> other) {
        MultiValueMap tmp = new MultiValueMap();
        for (Object key : saveWith.keySet()) {
            tmp.putAll(key, saveWith.getCollection(key));
        }
        tmp.put(relation, other);
        return new EntityUpdateBundle<T>(id, data, cls, tmp);
    }
}
