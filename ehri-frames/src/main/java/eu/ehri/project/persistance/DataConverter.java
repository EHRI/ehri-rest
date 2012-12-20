package eu.ehri.project.persistance;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;

public class DataConverter {

    /**
     * Convert a bundle to a generic data structure.
     * 
     * @param bundle
     * @return
     */
    public static Map<String, Object> bundleToData(Bundle bundle) {
        Map<String, Object> data = Maps.newHashMap();
        data.put(Converter.ID_KEY, bundle.getId());
        data.put(Converter.TYPE_KEY, bundle.getType().getName());
        data.put(Converter.DATA_KEY, bundle.getData());

        Map<String, List<Map<String, Object>>> relations = Maps.newHashMap();
        ListMultimap<String,Bundle> crelations = bundle.getRelations();
        for (String key : crelations.keySet()) {
            List<Map<String, Object>> rels = Lists.newArrayList();
            for (Bundle subbundle : crelations.get(key)) {
                rels.add(bundleToData(subbundle));
            }
            relations.put((String) key, rels);
        }
        data.put(Converter.REL_KEY, relations);
        return data;
    }
    
    /**
     * Convert a bundle to JSON.
     * 
     * @param bundle
     * @return
     * @throws SerializationError
     * 
     */
    public static String bundleToJson(Bundle bundle) throws SerializationError {
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
     * Convert some JSON into an EntityBundle.
     * 
     * @param json
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     * @throws DeserializationError
     */
    public static Bundle jsonToBundle(String json) throws DeserializationError {
        try {
            // FIXME: For some reason I can't fathom, a type reference is not working here.
            // When I add one in for HashMap<String,Object>, the return value of readValue
            // just seems to be Object ???
            ObjectMapper mapper = new ObjectMapper();
            return dataToBundle(mapper.readValue(json, Map.class));
        } catch (DeserializationError e) {
            throw e;
        } catch (Exception e) {
            throw new DeserializationError("Error decoding JSON", e);
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
    public static Bundle dataToBundle(Map<String, Object> data)
            throws DeserializationError {
        try {
            String id = (String) data.get(Converter.ID_KEY);
            EntityClass type = EntityClass.withName((String) data
                    .get(Converter.TYPE_KEY));
            Map<String, Object> props = (Map<String, Object>) data
                    .get(Converter.DATA_KEY);
            if (props == null)
                throw new DeserializationError("No item data map found");
            ListMultimap<String,Bundle> relationbundles = LinkedListMultimap.create();

            Map<String, List<Map<String, Object>>> relations = (Map<String, List<Map<String, Object>>>) data
                    .get(Converter.REL_KEY);
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
}
