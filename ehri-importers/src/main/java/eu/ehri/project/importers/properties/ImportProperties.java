/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.properties;

import java.util.Set;

/**
 * defines the interface for all import mappings
 * 
 * @author linda
 */
public interface ImportProperties {

    public String getProperty(String key);

    public boolean containsPropertyValue(String value);

    /**
     * 
     * @return returns the right-hand side of the properties file 
     */
    public Set<String> getAllNonAttributeValues();

    public boolean containsProperty(String key);

    public boolean hasAttributeProperty(String key);

    public String getAttributeProperty(String key);
}
