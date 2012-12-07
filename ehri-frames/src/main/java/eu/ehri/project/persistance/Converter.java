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
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
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
public final class Converter {

    public static final int DEFAULT_TRAVERSALS = 5;

    /**
     * Constant definitions
     */
    public static final String ID_KEY = "id";
    public static final String TYPE_KEY = "type";
    public static final String DATA_KEY = "data";
    public static final String REL_KEY = "relationships";

    /**
     * Lookup of entityType keys against their annotated class.
     */
    private final int maxTraversals;

    /**
     * Constructor.
     */
    public Converter() {
        this(DEFAULT_TRAVERSALS);
    }

    /**
     * Constructor which allows specifying depth of @Fetched traversals.
     * 
     * @param depth
     */
    public Converter(int depth) {
        this.maxTraversals = depth;
    }

    /**
     * Convert a vertex frame to a raw bundle of data.
     * 
     * @param item
     * @return
     * @throws SerializationError
     */
    public <T extends VertexFrame> Map<String, Object> vertexFrameToData(T item)
            throws SerializationError {
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
    public Bundle jsonToBundle(String json) throws DeserializationError {
        try {
            // FIXME: For some reason I can't fathom, a type reference is not working here.
            // When I add one in for HashMap<String,Object>, the return value of readValue
            // just seems to be Object ???
            ObjectMapper mapper = new ObjectMapper();
            return dataToBundle(mapper.readValue(json, Map.class));
        } catch (Exception e) {
            throw new DeserializationError("Error decoding JSON", e);
        }
    }

    /**
     * Serialise a vertex frame to JSON.
     * 
     * @param item
     * @return
     * @throws SerializationError
     */
    public <T extends VertexFrame> String vertexFrameToJson(T item) throws SerializationError {
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
    public String bundleToJson(Bundle bundle) throws SerializationError {
        Map<String, Object> data = bundleToData(bundle);
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Note: defaultPrettyPrintWriter has been replaced by
            // writerWithDefaultPrettyPrinter in newer versions of
            // Jackson, though not the one available in Neo4j.
            @SuppressWarnings("deprecation")
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
    public Bundle dataToBundle(Map<String, Object> data)
            throws DeserializationError {
        try {
            String id = (String) data.get(ID_KEY);
            EntityClass type = EntityClass.withName((String) data
                    .get(TYPE_KEY));
            Map<String, Object> props = (Map<String, Object>) data
                    .get(DATA_KEY);
            if (props == null)
                throw new DeserializationError("No item data map found");
            MultiValueMap relationbundles = new MultiValueMap();

            Map<String, List<Map<String, Object>>> relations = (Map<String, List<Map<String, Object>>>) data
                    .get(REL_KEY);
            if (relations != null) {
                for (Entry<String, List<Map<String, Object>>> entry : relations
                        .entrySet()) {
                    for (Map<String, Object> item : entry.getValue()) {
                        relationbundles.put(entry.getKey(), dataToBundle(item));
                    }
                }
            }

            return new Bundle(id, type, props, relationbundles);

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
    public Map<String, Object> bundleToData(Bundle bundle) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(ID_KEY, bundle.getId());
        data.put(TYPE_KEY, bundle.getType().getName());
        data.put(DATA_KEY, bundle.getData());

        Map<String, List<Map<String, Object>>> relations = new HashMap<String, List<Map<String, Object>>>();
        for (Object key : bundle.getRelations().keySet()) {
            List<Map<String, Object>> rels = new ArrayList<Map<String, Object>>();
            @SuppressWarnings("unchecked")
            Collection<Bundle> collection = bundle.getRelations()
                    .getCollection(key);
            for (Bundle subbundle : collection) {
                rels.add(bundleToData(subbundle));
            }
            relations.put((String) key, rels);
        }
        data.put(REL_KEY, relations);
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
    public <T extends VertexFrame> Bundle vertexFrameToBundle(T item)
            throws SerializationError {
        return vertexFrameToBundle(item, maxTraversals);
    }

    /**
     * Convert a VertexFrame into an EntityBundle that includes its @Fetch'd
     * relations.
     * 
     * @param item
     * @param depth
     * @return
     * @throws SerializationError
     */
    public <T extends VertexFrame> Bundle vertexFrameToBundle(T item, int depth)
            throws SerializationError {
        String id = (String) item.asVertex().getProperty(EntityType.ID_KEY);
        EntityClass type = EntityClass.withName((String) item.asVertex()
                .getProperty(EntityType.TYPE_KEY));
        MultiValueMap relations = getRelationData(item, depth,
                type.getEntityClass());
        return new Bundle(id, type,
                getVertexData(item.asVertex()), relations);
    }

    private <T extends VertexFrame> MultiValueMap getRelationData(T item, int depth,
            Class<?> cls) {
        MultiValueMap relations = new MultiValueMap();
        if (depth > 0) {
            Map<String, Method> fetchMethods = ClassUtils.getFetchMethods(cls);
            for (Map.Entry<String, Method> entry : fetchMethods.entrySet()) {

                // In order to avoid @Fetching the whole graph we track the
                // maxDepth parameter and reduce it for every traversal.
                // However the @Fetch annotation can also specify a non-default
                // depth, so we need to determine whatever is lower - the
                // current traversal count, or the annotation's count.
                Method method = entry.getValue();
                int nextDepth = Math.min(depth,
                        method.getAnnotation(Fetch.class).depth()) - 1;

                try {
                    Object result;
                    try {
                        result = method.invoke(item);
                    } catch (IllegalArgumentException e) {
                        String message = String
                                .format("When serializing a bundle, a method was called on an item it did not expect. Method name: %s, item class: %s",
                                        method.getName(),
                                        item.asVertex().getProperty(
                                                EntityType.TYPE_KEY));
                        throw new RuntimeException(message, e);
                    }
                    // The result of one of these fetchMethods should either be
                    // a single VertexFrame, or a Iterable<VertexFrame>.
                    if (result instanceof Iterable<?>) {
                        for (Object d : (Iterable<?>) result) {
                            relations.put(
                                    entry.getKey(),
                                    vertexFrameToBundle((VertexFrame) d,
                                            nextDepth));
                        }
                    } else {
                        // This relationship could be NULL if, e.g. a collection
                        // has no holder.
                        if (result != null)
                            relations.put(
                                    entry.getKey(),
                                    vertexFrameToBundle((VertexFrame) result,
                                            nextDepth));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(
                            "Unexpected error serializing VertexFrame", e);
                }
            }
        }
        return relations;
    }

    /**
     * Fetch a map of data from a vertex.
     */
    private Map<String, Object> getVertexData(Vertex item) {
        Map<String, Object> data = new HashMap<String, Object>();
        for (String key : item.getPropertyKeys()) {
            data.put(key, item.getProperty(key));
            if (!(key.equals(EntityType.ID_KEY) || key
                    .equals(EntityType.TYPE_KEY)))
                data.put(key, item.getProperty(key));
        }
        return data;
    }
}
