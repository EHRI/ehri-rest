package eu.ehri.project.persistance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.models.EntityClass;
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

class DataConverter {

    /**
     * Convert a bundle to a generic data structure.
     * 
     * @param bundle
     * @return
     */
    public static Map<String, Object> bundleToData(Bundle bundle) {
        Map<String, Object> data = Maps.newHashMap();
        data.put(Bundle.ID_KEY, bundle.getId());
        data.put(Bundle.TYPE_KEY, bundle.getType().getName());
        data.put(Bundle.DATA_KEY, bundle.getData());

        Map<String, List<Map<String, Object>>> relations = Maps.newHashMap();
        ListMultimap<String, Bundle> crelations = bundle.getRelations();
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
     * @throws DeserializationError
     */
    public static Bundle jsonToBundle(String json) throws DeserializationError {
        try {
            // FIXME: For some reason I can't fathom, a type reference is not
            // working here.
            // When I add one in for HashMap<String,Object>, the return value of
            // readValue
            // just seems to be Object ???
            ObjectMapper mapper = new ObjectMapper();
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
     * 
     * Prize to whomever can remove all the unchecked warnings. I don't really
     * know how else to do this otherwise.
     * 
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

        ListMultimap<String, Bundle> relationbundles = getRelationships(data);

        return new Bundle(id, type, properties, relationbundles);
    }

    /**
     * Extract relationships from the bundle data.
     * 
     * TODO: Should we throw an error if something
     * 
     * @param data
     * @return
     * @throws DeserializationError
     */
    private static ListMultimap<String, Bundle> getRelationships(Map<?, ?> data)
            throws DeserializationError {
        ListMultimap<String, Bundle> relationbundles = LinkedListMultimap
                .create();

        // It's okay to pass in a null value for relationships.
        Object relations = data.get(Bundle.REL_KEY);
        if (relations == null)
            return relationbundles;

        if (relations instanceof Map) {
            for (Entry<?, ?> entry : ((Map<?, ?>) relations).entrySet()) {
                if (entry.getValue() instanceof List<?>) {
                    for (Object item : (List<?>) entry.getValue()) {
                        relationbundles.put((String) entry.getKey(),
                                dataToBundle(item));
                    }
                }
            }
        } else {
            throw new DeserializationError(
                    "Relationships value should be a map type");
        }
        return relationbundles;
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
     * @param data
     * @return
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

    /**
     * Filter null values out of a map.
     * 
     * @param data
     * @return
     */
    private static Map<String, Object> sanitiseProperties(Map<?, ?> data) {
        Map<String, Object> cleaned = Maps.newHashMap();
        for (Entry<?, ?> entry : data.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() != null)
                cleaned.put((String) entry.getKey(), entry.getValue());
        }
        return cleaned;
    }

    /**
     * Convert a bundle to an XML document (currently with a very ad-hoc schema.)
     * @param bundle
     * @return
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

    public static String bundleToXmlString(Bundle bundle) {
        Document doc = bundleToXml(bundle);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            printDocument(doc, baos);
            return baos.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pretty-print an XML document.
     *
     * @param doc
     * @param out
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

    private static Element bundleDataToElement(final Document document, final Bundle bundle) {
        Element root = document.createElement("item");
        root.setAttribute(Bundle.ID_KEY, bundle.getId());
        root.setAttribute(Bundle.TYPE_KEY, bundle.getType().getName());
        Element data = document.createElement(Bundle.DATA_KEY);
        root.appendChild(data);
        for (Entry<String,Object> entry : bundle.getData().entrySet()) {
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
            for (Entry<String,Collection<Bundle>> entry : bundle.getRelations().asMap().entrySet()) {
                Element relation = document.createElement(entry.getKey());
                relations.appendChild(relation);
                for (Bundle relationBundle : entry.getValue()) {
                    relation.appendChild(bundleDataToElement(document, relationBundle));
                }
            }
        }

        return root;
    }

    private  static Element bundleDataValueToElement(final Document document, String key, Object value) {
        Element dataValue = document.createElement("property");
        dataValue.setAttribute("name", key);
        if (value instanceof String) {
            dataValue.setAttribute("type", "xs:string");
            dataValue.appendChild(document.createTextNode(String.valueOf(value)));
        } else if (value instanceof Integer) {
            dataValue.setAttribute("type", "xs:int");
            dataValue.appendChild(document.createTextNode(String.valueOf(value)));
        } else if (value instanceof Long) {
            dataValue.setAttribute("type", "xs:long");
            dataValue.appendChild(document.createTextNode(String.valueOf(value)));
        } else if (value instanceof Object[]) {
            dataValue.setAttribute("type", "array");
            Element seq = document.createElement("xs:sequence");
            for (Object item : (Object[])value) {
                seq.appendChild(bundleDataValueToElement(document, key, item));
            }

        } // Mmmn, what should we do for other types???
        else {
            dataValue.setAttribute("type", "unknown");
            dataValue.appendChild(document.createTextNode(String.valueOf(value)));
        }
        return dataValue;
    }
}
