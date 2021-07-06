package eu.ehri.project.importers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.importers.properties.XmlImportProperties;

import java.util.Optional;

/**
 * Options to control the behaviour of importers.
 */
public class ImportOptions {
    private static final Config config = ConfigFactory.load();

    public final boolean tolerant;
    public final boolean updates;
    public final String defaultLang;
    public final boolean useSourceId;
    public final XmlImportProperties properties;

    public static ImportOptions properties(String properties) {
        return create(false, false, false, null, properties);
    }

    /**
     * Basic factory method.
     */
    public static ImportOptions basic() {
        return create(false, false, false, null, (String) null);
    }

    private ImportOptions(boolean tolerant, boolean updates, boolean useSourceId, String defaultLang, String properties) {
        this(
                tolerant,
                updates,
                useSourceId,
                Optional.ofNullable(defaultLang).orElse(config.getString("io.import.defaultLang")),
                properties == null
                        ? new XmlImportProperties(config.getString("io.import.defaultProperties"))
                        : new XmlImportProperties(properties)
        );
    }

    private ImportOptions(boolean tolerant, boolean updates, boolean useSourceId, String defaultLang, XmlImportProperties properties) {
        this.tolerant = tolerant;
        this.updates = updates;
        this.useSourceId = useSourceId;
        this.defaultLang = defaultLang;
        this.properties = properties;
    }

    /**
     * Factory method.
     *
     * @param tolerant     do not error if individual items fail to validate
     * @param allowUpdates allow existing items to be updated
     * @param useSourceId  take account of the 'source file ID' (usually the EAD ID value) when updating
     *                     descriptions, in addition to the language code, allowing multiple descriptions
     *                     in the same language/script to exist
     * @param defaultLang  the default language code to use for newly-created items
     * @param properties   a property mapping configuration
     */
    public static ImportOptions create(boolean tolerant, boolean allowUpdates, boolean useSourceId, String defaultLang, String properties) {
        return new ImportOptions(tolerant, allowUpdates, useSourceId, defaultLang, properties);
    }

    public ImportOptions withProperties(String properties) {
        XmlImportProperties props = properties == null
                ? new XmlImportProperties(config.getString("io.import.defaultProperties"))
                : new XmlImportProperties(properties);
        return new ImportOptions(tolerant, updates, useSourceId, defaultLang, props);
    }

    public ImportOptions withUpdates(boolean updates) {
        return new ImportOptions(tolerant, updates, useSourceId, defaultLang, properties);
    }

    public ImportOptions withDefaultLang(String lang) {
        return new ImportOptions(tolerant, updates, useSourceId, lang, properties);
    }

    public ImportOptions withTolerant(boolean tolerant) {
        return new ImportOptions(tolerant, updates, useSourceId, defaultLang, properties);
    }

    public ImportOptions withUseSourceId(boolean merging) {
        return new ImportOptions(tolerant, updates, merging, defaultLang, properties);
    }
}
