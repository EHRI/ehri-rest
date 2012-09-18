package eu.ehri.project.persistance;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.map.MultiValueMap;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

/**
 * Class containing static methods to convert between FramedVertex instances,
 * EntityBundles, and raw data.
 * 
 * @author michaelb
 * 
 */
public class Converter {

    /**
     * Lookup of entityType keys against their annotated class.
     */
    private Map<String, Class<? extends VertexFrame>> classes;

    public Converter() {
        classes = getEntityClasses();
    }

    public <T extends VertexFrame> Map<String, Object> vertexFrameToData(
            VertexFrame item) throws SerializationError {
        return bundleToData(vertexFrameToBundle(item));
    }

    /**
     * Convert some JSON into an EntityBundle.
     * 
     * @param json
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     * @throws DeserializationError
     */
    public <T extends VertexFrame> EntityBundle<T> jsonToBundle(String json)
            throws DeserializationError {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> data;
        try {
            data = mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new DeserializationError("Error decoding JSON", e);
        }
        return dataToBundle(data);
    }

    /**
     * Serialise a vertex frame to JSON.
     * 
     * @param item
     * @return
     * @throws SerializationError
     */
    public <T extends VertexFrame> String vertexFrameToJson(VertexFrame item)
            throws SerializationError {
        return bundleToJson(vertexFrameToBundle(item));
    }

    /**
     * Convert a bundle to JSON.
     * 
     * @param bundle
     * @return
     * @throws SerializationError
     * 
     */
    public <T extends VertexFrame> String bundleToJson(EntityBundle<T> bundle)
            throws SerializationError {
        Map<String, Object> data = bundleToData(bundle);
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Note: defaultPrettyPrintWriter has been replaced by
            // writerWithDefaultPrettyPrinter in newer versions of 
            // Jackson, though not the one available in Neo4j.
            ObjectWriter writer = mapper.defaultPrettyPrintingWriter();
            return writer.writeValueAsString(data);
        } catch (Exception e) {
            throw new SerializationError("Error writing bundle to JSON", e);
        }
    }

    /**
     * Convert generic data into a bundle.
     * 
     * Prize to whomever can remove all the unchecked warnings. I don't really
     * know how else to do this otherwise.
     * 
     * @throws DeserializationError
     */
    @SuppressWarnings("unchecked")
    public <T extends VertexFrame> EntityBundle<T> dataToBundle(
            Map<String, Object> data) throws DeserializationError {
        try {
            Object id = data.get("id");
            Map<String, Object> props = (Map<String, Object>) data.get("data");
            if (props == null)
                throw new DeserializationError("No item data map found");
            String isa = (String) props.get(EntityType.KEY);
            if (isa == null)
                throw new DeserializationError(String.format(
                        "No '%s' attribute found in item data", EntityType.KEY));
            Class<T> cls = (Class<T>) classes.get(isa);
            if (cls == null)
                throw new DeserializationError(String.format(
                        "No class found for type %s type: '%s'",
                        EntityType.KEY, isa));
            MultiValueMap relationbundles = new MultiValueMap();

            Map<String, List<Map<String, Object>>> relations = (Map<String, List<Map<String, Object>>>) data
                    .get("relationships");
            if (relations != null) {
                for (Entry<String, List<Map<String, Object>>> entry : relations
                        .entrySet()) {
                    for (Map<String, Object> item : entry.getValue()) {
                        relationbundles.put(entry.getKey(), dataToBundle(item));
                    }
                }
            }

            return new EntityBundle<T>(id, props, cls, relationbundles);

        } catch (ClassCastException e) {
            throw new DeserializationError("Error deserializing data", e);
        }
    }

    /**
     * Convert a bundle to a generic data structure.
     * 
     * @param bundle
     * @return
     */
    public Map<String, Object> bundleToData(EntityBundle<?> bundle) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", bundle.getId());
        data.put("data", bundle.getData());

        Map<String, List<Map<String, Object>>> relations = new HashMap<String, List<Map<String, Object>>>();
        for (Object key : bundle.getRelations().keySet()) {
            List<Map<String, Object>> rels = new ArrayList<Map<String, Object>>();
            @SuppressWarnings("unchecked")
            Collection<EntityBundle<?>> collection = bundle.getRelations()
                    .getCollection(key);
            for (EntityBundle<?> subbundle : collection) {
                rels.add(bundleToData(subbundle));
            }
            relations.put((String) key, rels);
        }
        data.put("relationships", relations);
        return data;
    }

    /**
     * Convert a VertexFrame into an EntityBundle that includes its @Fetch'd
     * relations.
     * 
     * @param item
     * @return
     * @throws SerializationError
     */
    @SuppressWarnings("unchecked")
    public <T extends VertexFrame> EntityBundle<T> vertexFrameToBundle(
            VertexFrame item) throws SerializationError {
        String isa = (String) item.asVertex().getProperty(EntityType.KEY);
        if (isa == null)
            throw new SerializationError(String.format(
                    "No %s key found in Vertex Properties to denote its type.",
                    EntityType.KEY));
        Class<? extends VertexFrame> cls = classes.get(isa);
        if (cls == null)
            throw new SerializationError(String.format(
                    "No entity found for %s type '%s'", EntityType.KEY, isa));

        MultiValueMap relations = new MultiValueMap();
        Map<String, Method> fetchMethods = ClassUtils.getFetchMethods(cls);
        for (Map.Entry<String, Method> entry : fetchMethods.entrySet()) {
            try {
                Object result = entry.getValue().invoke(item);
                // The result of one of these fetchMethods should either be a
                // single VertexFrame, or a Iterable<VertexFrame>.
                if (result instanceof Iterable<?>) {
                    for (Object d : (Iterable<?>) result) {
                        relations.put(entry.getKey(),
                                vertexFrameToBundle((VertexFrame) d));
                    }
                } else {
                    // This relationship could be NULL if, e.g. a collection has
                    // no holder.
                    if (result != null)
                        relations.put(entry.getKey(),
                                vertexFrameToBundle((VertexFrame) result));
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Unexpected error serializing VertexFrame", e);
            }
        }
        return new EntityBundle<T>((Long) item.asVertex().getId(),
                getVertexData(item.asVertex()), (Class<T>) cls, relations);
    }

    /**
     * Fetch a map of data from a vertex.
     */
    private Map<String, Object> getVertexData(Vertex item) {
        Map<String, Object> data = new HashMap<String, Object>();
        for (String key : item.getPropertyKeys()) {
            data.put(key, item.getProperty(key));
        }
        return data;
    }

    /**
     * Load a lookup of entity type name against the corresponding class.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Class<? extends VertexFrame>> getEntityClasses() {
        // iterate through all the classes in our models package
        // and filter those that aren't extending VertexFrame
        Map<String, Class<? extends VertexFrame>> entitycls = new HashMap<String, Class<? extends VertexFrame>>();
        Class<?>[] classArray;
        try {
            classArray = ClassUtils.getClasses("eu.ehri.project.models");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unrecoverable problem loading EntityType classes", e);
        }
        List<Class<? extends VertexFrame>> vframes = new ArrayList<Class<? extends VertexFrame>>();
        for (Class<?> cls : classArray) {
            if (VertexFrame.class.isAssignableFrom(cls)) {
                // NB: This is the unchecked cast, but it should be safe due to
                // the
                // asAssignableFrom test.
                vframes.add((Class<? extends VertexFrame>) cls);
            }
        }

        for (Class<? extends VertexFrame> cls : vframes) {
            EntityType ann = cls.getAnnotation(EntityType.class);
            if (ann != null)
                entitycls.put(ann.value(), cls);
        }
        return entitycls;
    }
}
