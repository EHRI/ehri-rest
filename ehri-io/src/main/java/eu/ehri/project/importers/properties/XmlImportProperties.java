/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.importers.properties;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * wrapper class for the mapping of xml files to be imported.
 */
public class XmlImportProperties implements ImportProperties {


    private final Properties properties;

    public XmlImportProperties(String configFile) {
        properties = PropertyLoader.loadProperties(configFile);
    }

    /**
     * Get the value for the specified key, or null.
     */
    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get a set of property keys that have a given value
     *
     * @param value the value that requested properties must have
     * @return a set of key names that have the given value
     */
    Set<String> getPropertiesWithValue(String value) {
        return properties.entrySet().stream()
                .filter(e -> value.equals(e.getValue()))
                .map(e -> e.getKey().toString())
                .collect(Collectors.toSet());
    }

    public String getFirstPropertyWithValue(String value) {
        return properties.entrySet().stream()
                .filter(e -> value.equals(e.getValue()))
                .map(e -> e.getKey().toString())
                .findFirst().orElse(null);
    }

    @Override
    public Set<String> getAllNonAttributeValues() {
        return properties.entrySet().stream()
                .filter(e -> !e.getKey().toString().startsWith("@"))
                .map(e -> e.getValue().toString())
                .collect(Collectors.toSet());
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    @Override
    public boolean containsPropertyValue(String value) {
        return properties.containsValue(value);
    }

    @Override
    public boolean containsProperty(String path) {
        return properties.containsKey(path);
    }

    /**
     * @param key Attribute key name
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

    private static Properties loadPropertiesFromResource(String name, ClassLoader loader) {
        logger.debug("loading resource {}...", name);
        try (InputStream in = loader.getResourceAsStream(name)) {
            if (in != null) {
                Properties result = new Properties();
                result.load(in);
                return result;
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static Properties loadPropertiesFromFile(Path path) {
        logger.debug("loading file {}...", path.toUri());
        try (InputStream ios = Files.newInputStream(path)) {
            Properties result = new Properties();
            result.load(ios); // Can throw IOException
            return result;
        } catch (IOException e) {
            logger.error("Error loading properties file: {}: {}", path, e);
            return null;
        }
    }

    private static Properties loadPropertiesFromResourceOrFile(String name, ClassLoader loader) {
        Preconditions.checkNotNull(name, "Property resource name may not be null");
        if (loader == null) {
            return loadPropertiesFromResourceOrFile(name, ClassLoader.getSystemClassLoader());
        } else {
            Path path = Paths.get(name);
            if (Files.isRegularFile(path)) {
                return loadPropertiesFromFile(path);
            } else {
                return loadPropertiesFromResource(name, loader);
            }
        }
    }

    /**
     * Looks up a resource named 'name' in the classpath. The resource must map to a file with .properties extension.
     * The name is assumed to be absolute and can use either "/" or "." for package segment separation with an optional
     * leading "/" and optional ".properties" suffix. Thus, the following names refer to the same resource:
     * <p>
     * <pre>
     * some.pkg.Resource
     * some.pkg.Resource.properties
     * some/pkg/Resource
     * some/pkg/Resource.properties
     * /some/pkg/Resource
     * /some/pkg/Resource.properties
     * </pre>
     *
     * @param name   classpath resource name [may not be null]
     * @param loader the ClassLoader through which to load the resource [null is equivalent to the application loader]
     * @return resource converted to java.util.Properties
     * @throws IllegalArgumentException if the resource was not found
     */
    private static Properties loadProperties(String name, ClassLoader loader) throws IllegalArgumentException {
        Properties result = loadPropertiesFromResourceOrFile(name, loader);
        if (result == null) {
            String err = String.format("could not load [%s] as a classpath resource", name);
            throw new IllegalArgumentException(err);
        }
        return result;
    }

    /**
     * A convenience overload of {@link #loadProperties(String, ClassLoader)} that uses the current thread's context
     * ClassLoader.
     */
    static Properties loadProperties(String name) {
        return loadProperties(name, Thread.currentThread().getContextClassLoader());
    }
}