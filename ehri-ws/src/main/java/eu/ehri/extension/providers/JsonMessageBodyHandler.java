package eu.ehri.extension.providers;

import com.fasterxml.jackson.databind.ObjectMapper;


public interface JsonMessageBodyHandler {
    ObjectMapper mapper = new ObjectMapper();
}
