package eu.ehri.project.persistance.impl;

import java.text.MessageFormat;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.core.GraphManager;
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
    private final GraphManager manager;

    private final ListMultimap<String, String> errors = ArrayListMultimap
            .create();

    public BundleFieldValidator(GraphManager manager, Bundle bundle) {
        this.bundle = bundle;
        this.manager = manager;
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
        checkFields();
        checkEntityType();
        checkUniquenessOnUpdate();
        return errors;
    }

    /**
     * Validate bundle fields.
     * 
     * @return errors
     */
    public ListMultimap<String, String> validate() {
        checkFields();
        checkEntityType();
        checkUniqueness();
        return errors;
    }

    /**
     * Check a bundle's fields validate.
     */
    private void checkFields() {
        for (String key : ClassUtils.getMandatoryPropertyKeys(bundle.getBundleClass())) {
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

    /**
     * Check uniqueness constrains for a bundle's fields.
     */
    private void checkUniqueness() {
        for (String ukey : bundle.getUniquePropertyKeys()) {
            Object uval = bundle.getDataValue(ukey);
            if (uval != null) {
                CloseableIterable<Vertex> vertices = manager.getVertices(ukey, uval, bundle.getType());
                try {
                    if (vertices.iterator().hasNext()) {
                        errors.put(ukey, MessageFormat.format(Messages
                                .getString("BundleFieldValidator.uniquenessError"), uval));
                    }
                } finally {
                    vertices.close();
                }
            }
        }
    }

    /**
     * Check uniqueness constrains for a bundle's fields.
     */
    private void checkUniquenessOnUpdate() {
        for (String ukey : bundle.getUniquePropertyKeys()) {
            Object uval = bundle.getDataValue(ukey);
            if (uval != null) {
                CloseableIterable<Vertex> vertices = manager.getVertices(ukey, uval, bundle.getType());
                try {
                    if (vertices.iterator().hasNext()) {
                        Vertex v = vertices.iterator().next();
                        // If it's the same vertex, we don't have a problem...
                        if (!manager.getId(v).equals(bundle.getId())) {
                            errors.put(ukey, MessageFormat.format(Messages
                                    .getString("BundleFieldValidator.uniquenessError"), uval));

                        }
                    }
                } finally {
                    vertices.close();
                }
            }
        }
    }
}
