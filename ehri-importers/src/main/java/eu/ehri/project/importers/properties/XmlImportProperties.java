/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers.properties;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * wrapper class for the mapping of xml files to be imported. 
 * 
 * @author linda
 */
public class XmlImportProperties implements ImportProperties {

    private String configFileName;
    private Properties cachedProperties = null;

    public XmlImportProperties(String configFile) {
        setConfigFileName(configFile);
    }

    private Properties getProperties() {
        if (cachedProperties == null) {
            cachedProperties = PropertyLoader.loadProperties(this.configFileName);
        }
        return cachedProperties;
    }

    /**
     * Get the value for the specified key, or null.
     */
    @Override
    public String getProperty(String key) {
        return getProperties().getProperty(key);
    }
    public Set<String> getPropertiesWithValue(String value){
        Set<String> ps = new HashSet<String>();
        Properties p = getProperties();
        for(Object key : p.keySet()){
            if(value.equals(p.get(key).toString()))
                ps.add(key.toString());
        }
        return ps;
    }
    public String getFirstPropertyWithValue(String value){
        Set<String> ps = new HashSet<String>();
        Properties p = getProperties();
        for(Object key : p.keySet()){
            if(value.equals(p.get(key).toString()))
                return key.toString();
        }
        return null;
    }
    public Set<String> getAllNonAttributeProperties(){
        Set<String> ps = new HashSet<String>();
        Properties p = getProperties();
        for(Object key : p.keySet()){
            if( ! key.toString().startsWith("@"))
            ps.add(key.toString());
        }
        return ps;
    }

    public String getProperty(String key, String defaultValue) {
        return getProperties().getProperty(key, defaultValue);
    }

    @Override
    public boolean containsPropertyValue(String value) {
        return getProperties().containsValue(value);
    }

    private void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }
    
    
    //TODO: should be a deep copy
    public Set<Entry<Object, Object>> getEntries() {
        return getProperties().entrySet();
    }
    @Override
    public Set<String> getAllNonAttributeValues(){
        Set<String> values = new HashSet<String>();
        Properties p = getProperties();
        for(Object key : p.keySet()){
            if( ! key.toString().startsWith("@"))
            values.add(p.getProperty(key.toString()).toString());
        }
        return values;
    }

    @Override
    public boolean containsProperty(String path) {
        return getProperties().containsKey(path);
    }

    /**
     *
     * @param key
     * @return returns whether this key is mentioned as an attribute in the property file
     */
    @Override
    public boolean hasAttributeProperty(String key) {
        return containsProperty("@" + key);
    }

    @Override
    public String getAttributeProperty(String key) {
        return getProperty("@" + key);
    }
}

abstract class PropertyLoader {

    private static final Logger logger = LoggerFactory.getLogger(PropertyLoader.class);

    /**
     * Looks up a resource named 'name' in the classpath. The resource must map to a file with .properties extention.
     * The name is assumed to be absolute and can use either "/" or "." for package segment separation with an optional
     * leading "/" and optional ".properties" suffix. Thus, the following names refer to the same resource:
     *
     * <pre>
     * some.pkg.Resource
     * some.pkg.Resource.properties
     * some/pkg/Resource
     * some/pkg/Resource.properties
     * /some/pkg/Resource
     * /some/pkg/Resource.properties
     * </pre>
     *
     * @param name classpath resource name [may not be null]
     * @param loader classloader through which to load the resource [null is equivalent to the application loader]
     *
     * @return resource converted to java.util.Properties [may be null if the resource was not found and
     * THROW_ON_LOAD_FAILURE is false]
     * @throws IllegalArgumentException if the resource was not found and THROW_ON_LOAD_FAILURE is true
     */
    public static Properties loadProperties(String name, ClassLoader loader) {
        if (name == null) {
            throw new IllegalArgumentException("null input: name");
        }

        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        if (name.endsWith(SUFFIX)) {
            name = name.substring(0, name.length() - SUFFIX.length());
        }

        Properties result = null;

        InputStream in = null;
        try {
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }

            if (LOAD_AS_RESOURCE_BUNDLE) {
                name = name.replace('/', '.');
                // Throws MissingResourceException on lookup failures:
                final ResourceBundle rb = ResourceBundle.getBundle(name, Locale
                        .getDefault(), loader);

                result = new Properties();
                for (Enumeration<String> keys = rb.getKeys(); keys
                        .hasMoreElements();) {
                    final String key = keys.nextElement();
                    final String value = rb.getString(key);

                    result.put(key, value);
                }
            } else {

                name = name.replace('.', '/');
//				logger.info(name);

                if (!name.endsWith(SUFFIX)) {
                    name = name.concat(SUFFIX);
                }

                // Returns null on lookup failures:
                in = loader.getResourceAsStream(name);
                if (in != null) {
                    result = new Properties();
                    result.load(in); // Can throw IOException
                } else {
                    logger.error(name + " is geen file");
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result = null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignore) {
                }
            }
        }

        if (THROW_ON_LOAD_FAILURE && (result == null)) {
            throw new IllegalArgumentException("could not load ["
                    + name
                    + "]"
                    + " as "
                    + (LOAD_AS_RESOURCE_BUNDLE ? "a resource bundle"
                    : "a classloader resource"));
        }

        return result;
    }

    /**
     * A convenience overload of {@link #loadProperties(String, ClassLoader)} that uses the current thread's context
     * classloader.
     */
    public static Properties loadProperties(final String name) {
        return loadProperties(name, Thread.currentThread()
                .getContextClassLoader());
    }
    private static final boolean THROW_ON_LOAD_FAILURE = true;
    private static final boolean LOAD_AS_RESOURCE_BUNDLE = false;
    private static final String SUFFIX = ".properties";
}