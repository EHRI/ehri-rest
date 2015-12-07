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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.persistence.utils.DataUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class DataConverter {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    /**
     * Convert an error set to a generic data structure.
     *
     * @param errorSet An ErrorSet instance
     * @return A map containing the error set data
     */
    public static Map<String, Object> errorSetToData(ErrorSet errorSet) {
        Map<String, Object> data = Maps.newHashMap();
        data.put(ErrorSet.ERROR_KEY, errorSet.getErrors().asMap());
        Map<String, List<Map<String, Object>>> relations = Maps.newHashMap();
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
     * @param errorSet An ErrorSet instance
     * @return A JSON string representing the error set
     * @throws SerializationError
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
     * @param bundle The bundle
     * @return A data map
     */
    public static Map<String, Object> bundleToData(Bundle bundle) {
        Map<String, Object> data = Maps.newHashMap();
        data.put(Bundle.ID_KEY, bundle.getId());
        data.put(Bundle.TYPE_KEY, bundle.getType().getName());
        data.put(Bundle.DATA_KEY, bundle.getData());
        if (bundle.hasMetaData()) {
            data.put(Bundle.META_KEY, bundle.getMetaData());
        }
        Map<String, List<Map<String, Object>>> relations = Maps.newHashMap();
        Multimap<String, Bundle> crelations = bundle.getRelations();
        for (String key : crelations.keySet()) {
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
     * @param bundle The bundle
     * @return A JSON string representing the bundle
     * @throws SerializationError
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
     * Convert some JSON into an EntityBundle.
     *
     * @param inputStream An input stream containing JSON representing the bundle
     * @return The bundle
     * @throws DeserializationError
     */
    public static Bundle streamToBundle(InputStream inputStream) throws DeserializationError {
        try {
            return dataToBundle(mapper.readValue(inputStream, Map.class));
        } catch (IOException e) {
            e.printStackTrace();
            throw new DeserializationError("Error decoding JSON", e);
        }
    }

    /**
     * Convert some JSON into an EntityBundle.
     *
     * @param json A JSON string representing the bundle
     * @return The bundle
     * @throws DeserializationError
     */
    public static Bundle jsonToBundle(String json) throws DeserializationError {
        try {
            return dataToBundle(mapper.readValue(json, Map.class));
        } catch (DeserializationError e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DeserializationError("Error decoding JSON", e);
        }
    }

    /**
     * Convert generic data into a bundle.
     * <p/>
     * Prize to whomever can remove all the unchecked warnings. I don't really
     * know how else to do this otherwise.
     * <p/>
     * NB: We also strip out all NULL property values at this stage.
     *
     * @throws DeserializationError
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
        return new Bundle(id, type, properties, getRelationships(data));
    }

    /**
     * Extract relationships from the bundle data.
     *
     * @param data A plain map
     * @return A
     * @throws DeserializationError
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
        if (props != null && props instanceof Map) {
            return sanitiseProperties((Map<?, ?>) props);
        }
        throw new DeserializationError(
                "No item data map found or data value not a map type.");
    }

    /**
     * Get the type key, which should correspond the one of the EntityTypes enum
     * values.
     *
     * @param data The data, as an untyped map
     * @return A type, extracted from the data
     * @throws DeserializationError
     */
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
            if (value != null && !DataUtils.isEmptySequence(value)) {
                cleaned.put((String) entry.getKey(), entry.getValue());
            }
        }
        return cleaned;
    }

    /**
     * Convert a bundle to an XML document (currently with a very ad-hoc schema.)
     *
     * @param bundle The bundle
     * @return An XML document
     */
    public static Document bundleToXml(Bundle bundle) {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element root = bundleDataToElement(doc, bundle);
            doc.appendChild(root);
            return doc;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Pretty-print a bundle as XML to a string.
     *
     * @param bundle a bundle
     * @return an XML string
     */
    public static String bundleToXmlString(Bundle bundle) {
        Document doc = bundleToXml(bundle);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            printDocument(doc, baos);
            return baos.toString("UTF-8");
        } catch (IOException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pretty-print an XML document.
     *
     * @param doc The document
     * @param out An OutputStream instance
     * @throws IOException
     * @throws TransformerException
     */
    public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }

    private static Element bundleDataToElement(Document document, Bundle bundle) {
        Element root = document.createElement("item");
        root.setAttribute(Bundle.ID_KEY, bundle.getId());
        root.setAttribute(Bundle.TYPE_KEY, bundle.getType().getName());
        Element data = document.createElement(Bundle.DATA_KEY);
        root.appendChild(data);
        for (Entry<String, Object> entry : bundle.getData().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                Element dataValue = bundleDataValueToElement(document, key, value);
                data.appendChild(dataValue);
            }
        }
        if (!bundle.getRelations().isEmpty()) {
            Element relations = document.createElement(Bundle.REL_KEY);
            root.appendChild(relations);
            for (Entry<String, Collection<Bundle>> entry : bundle.getRelations().asMap().entrySet()) {
                Element relation = document.createElement(entry.getKey());
                relations.appendChild(relation);
                for (Bundle relationBundle : entry.getValue()) {
                    relation.appendChild(bundleDataToElement(document, relationBundle));
                }
            }
        }

        return root;
    }

    private static Element bundleDataValueToElement(Document document, String key, Object value) {
        if (value instanceof Object[]) {
            Element dataValue = document.createElement("propertySequence");
            for (Object item : (Object[]) value) {
                dataValue.appendChild(bundleDataValueToElement(document, key, item));
            }
            return dataValue;
        } else {
            Element dataValue = document.createElement("property");
            if (value instanceof String) {
                dataValue.setAttribute("name", key);
                dataValue.setAttribute("type", "xs:string");
                dataValue.appendChild(document.createTextNode(String.valueOf(value)));
            } else if (value instanceof Integer) {
                dataValue.setAttribute("name", key);
                dataValue.setAttribute("type", "xs:int");
                dataValue.appendChild(document.createTextNode(String.valueOf(value)));
            } else if (value instanceof Long) {
                dataValue.setAttribute("name", key);
                dataValue.setAttribute("type", "xs:long");
                dataValue.appendChild(document.createTextNode(String.valueOf(value)));
            } else { // Mmmn, what should we do for other types???
                dataValue.setAttribute("type", "unknown");
                dataValue.appendChild(document.createTextNode(String.valueOf(value)));
            }
            return dataValue;
        }
    }
}
