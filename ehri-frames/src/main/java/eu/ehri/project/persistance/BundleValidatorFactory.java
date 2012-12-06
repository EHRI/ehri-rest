package eu.ehri.project.persistance;

import eu.ehri.project.persistance.impl.BundleFieldValidator;

/**
 * Factory class for creating bundle validators.
 * 
 * @author mike
 *
 */
public class BundleValidatorFactory {
    public static BundleValidator getInstance(Bundle<?> bundle) {
        return new BundleFieldValidator(bundle);
    }
}
