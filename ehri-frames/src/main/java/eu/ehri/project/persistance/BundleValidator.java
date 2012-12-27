package eu.ehri.project.persistance;

import com.google.common.collect.ListMultimap;

/**
 * Interface for Bundle Validation classes.
 * 
 * @author mike
 *
 */
public interface BundleValidator {
    public ListMultimap<String, String> validate();
    public ListMultimap<String, String> validateForUpdate();
}
