/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.properties;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author linda
 */
public class PropertiesRow {

    Map<String, String> properties;

    protected PropertiesRow() {
        properties = new HashMap<String, String>();
    }

    @Override
    protected PropertiesRow clone(){
        PropertiesRow p = new PropertiesRow();
        p.properties.putAll(properties);
        return p;
    }
    /**
     * only add key value pair if value is no empty String
     * @param key
     * @param value 
     */
    protected PropertiesRow add(String key, String value) {
        if (!value.equals("")) {
            properties.put(key, value);
        }
        return this;
    }

    /**
     * 
     * @param key
     * @return return value if exists, null otherwise 
     */
    protected String get(String key) {
        return properties.get(key);
    }
}
