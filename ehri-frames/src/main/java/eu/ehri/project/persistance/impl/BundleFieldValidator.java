package eu.ehri.project.persistance.impl;

import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.persistance.BundleValidator;
import eu.ehri.project.persistance.Bundle;

/**
 * Class responsible for validating bundles.
 * 
 * @author mike
 * 
 */
public final class BundleFieldValidator implements BundleValidator {

    private static final String MISSING_PROPERTY = "Missing mandatory field";
    private static final String EMPTY_VALUE = "No value given for mandatory field";
    private static final String INVALID_ENTITY = "No EntityType annotation";

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
            errors.put("id", 
                    "No identifier given for update operation.");
        return validate();
    }

    /**
     * Validate bundle fields.
     * 
     * @return errors
     */
    public ListMultimap<String, String> validate() {
        checkFields();
        checkIsA();
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
            errors.put(name, MISSING_PROPERTY);
        } else {
            Object value = data.get(name);
            if (value == null) {
                errors.put(name, EMPTY_VALUE);
            } else if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    errors.put(name, EMPTY_VALUE);
                }
            }
        }
    }

    /**
     * @param data
     * @param cls
     * @param errors
     */
    private void checkIsA() {
        EntityType annotation = bundle.getBundleClass().getAnnotation(
                EntityType.class);
        if (annotation == null) {
            errors.put("class", String.format("%s: '%s'", INVALID_ENTITY,
                    bundle.getBundleClass().getName()));
        }
    }

}
