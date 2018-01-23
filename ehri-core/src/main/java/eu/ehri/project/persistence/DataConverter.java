/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.persistence;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.tinkerpop.blueprints.CloseableIterable;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class DataConverter {

    private static final JsonFactory factory = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper(factory);
    private static final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    static {
        SimpleModule bundleModule = new SimpleModule();
        bundleModule.addDeserializer(Bundle.class, new BundleDeserializer());
        mapper.registerModule(bundleModule);
    }

    /**
     * Convert an error set to a generic data structure.
     *
     * @param errorSet an ErrorSet instance
     * @return a map containing the error set data
     */
    public static Map<String, Object> errorSetToData(ErrorSet errorSet) {
        Map<String, Object> data = Maps.newHashMap();
        data.put(ErrorSet.ERROR_KEY, errorSet.getErrors().asMap());
        Map<String, List<Map<String, Object>>> relations = Maps.newLinkedHashMap();
        Multimap<String, ErrorSet> crelations = errorSet.getRelations();
        for (String key : crelations.keySet()) {
            List<Map<String, Object>> rels = Lists.newArrayList();
            for (ErrorSet subbundle : crelations.get(key)) {
                rels.add(errorSetToData(subbundle));
            }
            relations.put(key, rels);
        }
        data.put(ErrorSet.REL_KEY, relations);
        return data;
    }

    /**
     * Convert an error set to JSON.
     *
     * @param errorSet an ErrorSet instance
     * @return a JSON string representing the error set
     */
    public static String errorSetToJson(ErrorSet errorSet) throws SerializationError {
        try {
            Map<String, Object> data = errorSetToData(errorSet);
            return writer.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new SerializationError("Error writing errorSet to JSON", e);
        }
    }

    /**
     * Convert a bundle to a generic data structure.
     *
     * @param bundle the bundle
     * @return a data map
     */
    public static Map<String, Object> bundleToData(Bundle bundle) {
        Map<String, Object> data = Maps.newLinkedHashMap();
        data.put(Bundle.ID_KEY, bundle.getId());
        data.put(Bundle.TYPE_KEY, bundle.getType().getName());
        data.put(Bundle.DATA_KEY, bundle.getData());
        if (bundle.hasMetaData()) {
            data.put(Bundle.META_KEY, bundle.getMetaData());
        }
        Map<String, List<Map<String, Object>>> relations = Maps.newLinkedHashMap();
        Multimap<String, Bundle> crelations = bundle.getRelations();
        List<String> sortedKeys = Ordering.natural().sortedCopy(crelations.keySet());
        for (String key : sortedKeys) {
            List<Map<String, Object>> rels = Lists.newArrayList();
            for (Bundle subbundle : crelations.get(key)) {
                rels.add(bundleToData(subbundle));
            }
            relations.put(key, rels);
        }
        data.put(Bundle.REL_KEY, relations);
        return data;
    }

    /**
     * Convert a bundle to JSON.
     *
     * @param bundle the bundle
     * @return a JSON string representing the bundle
     */
    public static String bundleToJson(Bundle bundle) throws SerializationError {
        try {
            Map<String, Object> data = bundleToData(bundle);
            return writer.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new SerializationError("Error writing bundle to JSON", e);
        }
    }

    /**
     * Return a JSON-Patch representation of the difference between
     * two bundles. Metadata is included.
     *
     * @param source the source bundle
     * @param target the target bundle
     * @return a JSON-Patch, as a string
     */
    public static String diffBundles(Bundle source, Bundle target) {
        try {
            JsonNode diff = JsonDiff.asJson(
                    mapper.valueToTree(source), mapper.valueToTree(target));
            return writer.writeValueAsString(diff);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert some JSON into an EntityBundle.
     *
     * @param inputStream an input stream containing JSON representing the bundle
     * @return the bundle
     *                              a valid bundle
     */
    public static Bundle streamToBundle(InputStream inputStream) throws DeserializationError {
        try {
            return mapper.readValue(inputStream, Bundle.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new DeserializationError("Error decoding JSON", e);
        }
    }

    /**
     * Write a bundle to a JSON stream.
     *
     * @param bundle       the bundle
     * @param outputStream the stream
     */
    public static void bundleToStream(Bundle bundle, OutputStream outputStream) throws SerializationError {
        try {
            mapper.writeValue(outputStream, bundle);
        } catch (IOException e) {
            e.printStackTrace();
            throw new SerializationError("Error encoding JSON", e);
        }
    }

    /**
     * Parse an input stream containing a JSON array of bundle objects into
     * an iterable of bundles.
     *
     * @param inputStream a JSON input stream
     * @return an iterable of bundle objects
     *                              a valid bundle
     */
    public static CloseableIterable<Bundle> bundleStream(InputStream inputStream) throws DeserializationError {
        Preconditions.checkNotNull(inputStream);
        try {
            final JsonParser parser = factory
                    .createParser(new InputStreamReader(inputStream, Charsets.UTF_8));
            JsonToken jsonToken = parser.nextValue();
            if (!parser.isExpectedStartArrayToken()) {
                throw new DeserializationError("Stream should be an array of objects, was: " + jsonToken);
            }
            final Iterator<Bundle> iterator = parser.nextValue() == JsonToken.END_ARRAY
                    ? Collections.<Bundle>emptyIterator()
                    : parser.readValuesAs(Bundle.class);
            return new CloseableIterable<Bundle>() {
                @Override
                public void close() {
                    try {
                        parser.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public Iterator<Bundle> iterator() {
                    return iterator;
                }
            };
        } catch (IOException e) {
            throw new DeserializationError("Error reading JSON", e);
        }
    }

    /**
     * Convert some JSON into an EntityBundle.
     *
     * @param json a JSON string representing the bundle
     * @return the bundle
     *                              a valid bundle
     */
    public static Bundle jsonToBundle(String json) throws DeserializationError {
        try {
            return mapper.readValue(json, Bundle.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DeserializationError("Error decoding JSON", e);
        }
    }

    /**
     * Convert generic data into a bundle.
     * <p>
     * Prize to whomever can remove all the unchecked warnings. I don't really
     * know how else to do this otherwise.
     * <p>
     * NB: We also strip out all NULL property values at this stage.
     *
     * @param rawData an map object
     * @return a bundle
     *                              a valid bundle
     */
    public static Bundle dataToBundle(Object rawData)
            throws DeserializationError {

        // Check what we've been given is actually a Map...
        if (!(rawData instanceof Map<?, ?>))
            throw new DeserializationError("Bundle data must be a map value.");

        Map<?, ?> data = (Map<?, ?>) rawData;
        String id = (String) data.get(Bundle.ID_KEY);
        EntityClass type = getType(data);

        // Guava's immutable collections don't allow null values.
        // Since Neo4j doesn't either it's safest to trip these out
        // at the deserialization stage. I can't think of a use-case
        // where we'd need them.
        Map<String, Object> properties = getSanitisedProperties(data);
        return Bundle.of(id, type, properties, getRelationships(data));
    }

    /**
     * Extract relationships from the bundle data.
     *
     * @param data a plain map
     * @return a multi-map of string -> bundle list
     *                              valid relationships
     */
    private static Multimap<String, Bundle> getRelationships(Map<?, ?> data)
            throws DeserializationError {
        Multimap<String, Bundle> relationBundles = ArrayListMultimap
                .create();

        // It's okay to pass in a null value for relationships.
        Object relations = data.get(Bundle.REL_KEY);
        if (relations == null)
            return relationBundles;

        if (relations instanceof Map) {
            for (Entry<?, ?> entry : ((Map<?, ?>) relations).entrySet()) {
                if (entry.getValue() instanceof List<?>) {
                    for (Object item : (List<?>) entry.getValue()) {
                        relationBundles.put((String) entry.getKey(),
                                dataToBundle(item));
                    }
                }
            }
        } else {
            throw new DeserializationError(
                    "Relationships value should be a map type");
        }
        return relationBundles;
    }

    private static Map<String, Object> getSanitisedProperties(Map<?, ?> data)
            throws DeserializationError {
        Object props = data.get(Bundle.DATA_KEY);
        if (props != null) {
            if (props instanceof Map) {
                return sanitiseProperties((Map<?, ?>) props);
            } else {
                throw new DeserializationError(
                        "Data value not a map type! " + props.getClass().getSimpleName());
            }
        } else {
            return Maps.newHashMap();
        }
    }

    private static EntityClass getType(Map<?, ?> data)
            throws DeserializationError {
        try {
            return EntityClass.withName((String) data.get(Bundle.TYPE_KEY));
        } catch (IllegalArgumentException e) {
            throw new DeserializationError("Bad or unknown type key: "
                    + data.get(Bundle.TYPE_KEY));
        }
    }

    private static Map<String, Object> sanitiseProperties(Map<?, ?> data) {
        Map<String, Object> cleaned = Maps.newHashMap();
        for (Entry<?, ?> entry : data.entrySet()) {
            Object value = entry.getValue();
            // Allow any null value, as long as it's not an empty array
            if (!isEmptySequence(value)) {
                cleaned.put((String) entry.getKey(), entry.getValue());
            }
        }
        return cleaned;
    }

    /**
     * Ensure a value isn't an empty array or list, which will
     * cause Neo4j to barf.
     *
     * @param value A unknown object
     * @return If the object is a sequence type, and is empty
     */
    static boolean isEmptySequence(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof Object[]) {
            return ((Object[]) value).length == 0;
        } else if (value instanceof Collection<?>) {
            return ((Collection) value).isEmpty();
        } else if (value instanceof Iterable<?>) {
            return !((Iterable) value).iterator().hasNext();
        }
        return false;
    }
}
