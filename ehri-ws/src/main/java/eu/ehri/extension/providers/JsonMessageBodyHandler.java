package eu.ehri.extension.providers;

import com.fasterxml.jackson.databind.ObjectMapper;


interface JsonMessageBodyHandler {
    ObjectMapper mapper = new ObjectMapper();
}
