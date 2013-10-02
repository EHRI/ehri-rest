package eu.ehri.project.persistance;

import eu.ehri.project.core.GraphManager;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.impl.BundleFieldValidator;

/**
 * Factory class for creating bundle validators.
 * 
 * @author mike
 *
 */
public class BundleValidatorFactory {
    public static BundleValidator getInstance(GraphManager manager, Bundle bundle, PermissionScope scope) {
        return new BundleFieldValidator(manager, bundle, scope);
    }
}
