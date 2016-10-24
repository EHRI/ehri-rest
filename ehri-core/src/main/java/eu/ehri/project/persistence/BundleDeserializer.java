package eu.ehri.project.persistence;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import eu.ehri.project.exceptions.DeserializationError;

import java.io.IOException;
import java.util.Map;

public class BundleDeserializer extends JsonDeserializer<Bundle> {
    private static final TypeReference<Map<String, Object>> tref = new TypeReference<Map<String, Object>>() {
    };

    @Override
    public Bundle deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        try {
            return DataConverter.dataToBundle(parser.readValueAs(tref));
        } catch (DeserializationError deserializationError) {
            throw new IOException(deserializationError);
        }
    }
}
