package eu.ehri.project.persistance;

import eu.ehri.project.exceptions.ValidationError;

/**
 * Interface for Bundle Validation classes.
 * 
 * @author mike
 *
 */
public interface BundleValidator {
    public void validate() throws ValidationError;
    public void validateForInsert() throws ValidationError;
    public void validateForUpdate() throws ValidationError;
}
