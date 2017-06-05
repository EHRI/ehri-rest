package eu.ehri.project.importers.base;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import eu.ehri.project.importers.properties.NodeProperties;
import eu.ehri.project.models.EntityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class ImportHelper {
    private static final String NODE_PROPERTIES = "allowedNodeProperties.csv";
    private static final Logger logger = LoggerFactory.getLogger(ImportHelper.class);
    private static final Joiner stringJoiner = Joiner.on("\n\n").skipNulls();

    private final NodeProperties nodeProperties;

    public ImportHelper() {
        nodeProperties = loadNodeProperties();
    }

    /**
     * only properties that have the multivalued-status can actually be multivalued. all other properties will be
     * flattened by this method.
     *
     * @param key    a property key
     * @param value  a property value
     * @param entity the EntityClass with which this frameMap must comply
     */
    public Object flattenNonMultivaluedProperties(String key, Object value, EntityClass entity) {
        if (value instanceof List
                && !(nodeProperties.hasProperty(entity.getName(), key)
                && nodeProperties.isMultivaluedProperty(entity.getName(), key))) {
            logger.trace("Flattening array property value: {}: {}", key, value);
            return stringJoiner.join((List) value);
        } else {
            return value;
        }
    }


    // Helpers

    private NodeProperties loadNodeProperties() {
        try (InputStream fis = getClass().getClassLoader().getResourceAsStream(NODE_PROPERTIES);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charsets.UTF_8))) {
            NodeProperties nodeProperties = new NodeProperties();
            String headers = br.readLine();
            nodeProperties.setTitles(headers);

            String line;
            while ((line = br.readLine()) != null) {
                nodeProperties.addRow(line);
            }
            return nodeProperties;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (NullPointerException npe) {
            throw new RuntimeException("Missing or empty properties file: " + NODE_PROPERTIES);
        }
    }
}
