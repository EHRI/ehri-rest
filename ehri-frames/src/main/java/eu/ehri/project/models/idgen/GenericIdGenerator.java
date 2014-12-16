package eu.ehri.project.models.idgen;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.google.common.collect.ListMultimap;
import eu.ehri.project.persistence.Bundle;

import java.util.List;
import java.util.UUID;

/**
 * Generates a generic ID for tertiary node types.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public enum GenericIdGenerator implements IdGenerator {

    INSTANCE;

    // NB: We use a time-based UUID generator here because
    // sequential UUIDs prevent index fragmentation.
    private static final TimeBasedGenerator timeBasedGenerator
            = Generators.timeBasedGenerator();

    public ListMultimap<String, String> handleIdCollision(List<String> scopeIds, Bundle bundle) {
        throw new RuntimeException(String.format("Index collision generating identifier for item type '%s' with data: '%s'",
                bundle.getType().getName(), bundle));
    }

    /**
     * Generates a random String.
     *
     * @param scopeIds array of scope ids
     * @param bundle   The entity's bundle data
     * @return A generated ID string
     */
    public String generateId(Iterable<String> scopeIds, Bundle bundle) {
        return getIdBase(bundle);
    }

    /**
     * Return the base data for the id, sans scoping.
     * @param bundle The entity's bundle.
     * @return The base id string.
     */
    public String getIdBase(Bundle bundle) {
        return getTimeBasedUUID().toString();
    }

    /**
     * Get a new time-based UUID.
     * @return A time based UUID.
     */
    public static UUID getTimeBasedUUID() {
        return timeBasedGenerator.generate();
    }
}
