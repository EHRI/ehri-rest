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

	/**
     * Get the value for the specified key.
     */
    public String getProperty(String key);

    /**
     * See whether a value exists in the properties file.
     * 
     * @param value a value to look for
     * @return true when found, false when it doesn't exist in the file
     */
    public boolean containsPropertyValue(String value);

    /**
     * 
     * @return the right-hand side of the properties file 
     */
    public Set<String> getAllNonAttributeValues();

    public boolean containsProperty(String key);

    public boolean hasAttributeProperty(String key);

    /**
     * Get the value for the given attribute name.
     */
    public String getAttributeProperty(String key);
}
