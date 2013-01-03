package eu.ehri.project.persistance.impl;

import java.text.MessageFormat;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleValidator;

/**
 * Class responsible for validating bundles.
 * 
 * @author mike
 * 
 */
public final class BundleFieldValidator implements BundleValidator {

    private final Bundle bundle;

    private final ListMultimap<String, String> errors = ArrayListMultimap
            .create();

    public BundleFieldValidator(Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for updating in the graph.
     * 
     * @return errors
     * 
     */
    public ListMultimap<String, String> validateForUpdate() {
        if (bundle.getId() == null)
            errors.put(Bundle.ID_KEY, Messages
                    .getString("BundleFieldValidator.missingIdForUpdate")); //$NON-NLS-1$
        return validate();
    }

    /**
     * Validate bundle fields.
     * 
     * @return errors
     */
    public ListMultimap<String, String> validate() {
        checkFields();
        checkEntityType();
        return errors;
    }

    /**
     * @param data
     * @param cls
     * @param errors
     */
    private void checkFields() {
        for (String key : ClassUtils.getPropertyKeys(bundle.getBundleClass())) {
            checkField(key);
        }
    }

    /**
     * Check the data holds a given field, accounting for the
     * 
     * @param name
     */
    private void checkField(String name) {
        Map<String, Object> data = bundle.getData();
        if (!data.containsKey(name)) {
            errors.put(name,
                    Messages.getString("BundleFieldValidator.missingField"));
        } else {
            Object value = data.get(name);
            if (value == null) {
                errors.put(name,
                        Messages.getString("BundleFieldValidator.emptyField"));
            } else if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    errors.put(name, Messages
                            .getString("BundleFieldValidator.emptyField"));
                }
            }
        }
    }

    /**
     * Check entity type annotation.
     */
    private void checkEntityType() {
        EntityType annotation = bundle.getBundleClass().getAnnotation(
                EntityType.class);
        if (annotation == null) {
            errors.put(Bundle.TYPE_KEY, MessageFormat.format(Messages
                    .getString("BundleFieldValidator.missingTypeAnnotation"), //$NON-NLS-1$
                    bundle.getBundleClass().getName()));
        }
    }

}
