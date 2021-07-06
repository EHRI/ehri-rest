package eu.ehri.project.importers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.importers.properties.XmlImportProperties;

import java.util.Optional;

public class ImportOptions {
    private static final Config config = ConfigFactory.load();

    public final boolean tolerant;
    public final boolean updates;
    public final String defaultLang;
    public final boolean merging;
    public final XmlImportProperties properties;

    public static ImportOptions properties(String properties) {
        return new ImportOptions(false, false, false, null, properties);
    }

    public static ImportOptions basic() {
        return new ImportOptions(false, false, false, null, (String)null);
    }

    public ImportOptions(boolean tolerant, boolean updates, boolean merging, String defaultLang, String properties) {
        this(
                tolerant,
                updates,
                merging,
                Optional.ofNullable(defaultLang).orElse(config.getString("io.import.defaultLang")),
                properties == null
                        ? new XmlImportProperties(config.getString("io.import.defaultProperties"))
                        : new XmlImportProperties(properties)
        );
    }

    private ImportOptions(boolean tolerant, boolean updates, boolean merging, String defaultLang, XmlImportProperties properties) {
        this.tolerant = tolerant;
        this.updates = updates;
        this.merging = merging;
        this.defaultLang = defaultLang;
        this.properties = properties;
    }

    public ImportOptions withProperties(String properties) {
        XmlImportProperties props = properties == null
                ? new XmlImportProperties(config.getString("io.import.defaultProperties"))
                : new XmlImportProperties(properties);
        return new ImportOptions(tolerant, updates, merging, defaultLang, props);
    }

    public ImportOptions withUpdates(boolean updates) {
        return new ImportOptions(tolerant, updates, merging, defaultLang, properties);
    }

    public ImportOptions withDefaultLang(String lang) {
        return new ImportOptions(tolerant, updates, merging, lang, properties);
    }

    public ImportOptions withTolerant(boolean tolerant) {
        return new ImportOptions(tolerant, updates, merging, defaultLang, properties);
    }

    public ImportOptions withMerging(boolean merging) {
        return new ImportOptions(tolerant, updates, merging, defaultLang, properties);
    }
}
