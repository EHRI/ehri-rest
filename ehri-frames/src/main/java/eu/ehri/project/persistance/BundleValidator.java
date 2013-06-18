package eu.ehri.project.persistance;

import com.google.common.collect.ListMultimap;
import eu.ehri.project.exceptions.BundleError;
import eu.ehri.project.exceptions.ValidationError;

/**
 * Interface for Bundle Validation classes.
 * 
 * @author mike
 *
 */
public interface BundleValidator {
    public ListMultimap<String,String> validate();
    public ListMultimap<String,String> validateForUpdate();
    public void validateTree() throws ValidationError;
    public void validateTreeForUpdate() throws ValidationError;
}
