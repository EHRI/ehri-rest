package eu.ehri.extension.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;


public interface JsonMessageBodyHandler {
    ObjectMapper mapper = JsonMapper.builder().build();
}
