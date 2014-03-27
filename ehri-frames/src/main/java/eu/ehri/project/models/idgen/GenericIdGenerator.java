package eu.ehri.project.models.idgen;

import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.google.common.collect.ListMultimap;
import eu.ehri.project.persistence.Bundle;
import com.fasterxml.uuid.Generators;

/**
 * Generates a generic ID for tertiary node types.
 *
 * @author michaelb
 */
public enum GenericIdGenerator implements IdGenerator {

    INSTANCE;

    public static TimeBasedGenerator timeBasedGenerator
            = Generators.timeBasedGenerator();

    public ListMultimap<String, String> handleIdCollision(Iterable<String> scopeIds, Bundle bundle) {
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
        return timeBasedGenerator.generate().toString();
    }
}
