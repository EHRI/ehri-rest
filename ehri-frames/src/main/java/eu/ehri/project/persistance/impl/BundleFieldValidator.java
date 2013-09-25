package eu.ehri.project.persistance.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.BundleError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.BundleValidator;
import eu.ehri.project.persistance.BundleValidatorFactory;

import java.text.MessageFormat;
import java.util.Map;

/**
 * Class responsible for validating bundles.
 *
 * FIXME: This currently duplicates much of the functionality in the
 * (not very nice) BundleDAO code. What it doesn't (yet) do is handle
 * id collisions and integrity errors, but since this is more or less
 * exactly what we do here, it should be extended to do so.
 * 
 * @author mike
 * 
 */
public final class BundleFieldValidator implements BundleValidator {

    private final Bundle bundle;
    private final GraphManager manager;

    private final ListMultimap<String, String> errors = ArrayListMultimap
            .create();
    private final ListMultimap<String, BundleError> childErrors = ArrayListMultimap
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
    public ListMultimap<String,String> validateForUpdate() {
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
    public ListMultimap<String,String> validate() {
        checkFields();
        checkEntityType();
        checkUniqueness();
        return errors;
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for updating in the graph.
     * 
     * @return errors
     * 
     */
    public void validateTreeForUpdate() throws ValidationError {
        if (bundle.getId() == null)
            errors.put(Bundle.ID_KEY, Messages
                    .getString("BundleFieldValidator.missingIdForUpdate")); //$NON-NLS-1$
        checkFields();
        checkEntityType();
        checkUniquenessOnUpdate();
        checkChildren(true);
    }

    /**
     * Validate bundle fields.
     * 
     * @return errors
     */
    public void validateTree() throws ValidationError {
        checkFields();
        checkEntityType();
        checkUniqueness();
        checkChildren(false);

        if (!errors.isEmpty() || hasNestedErrors(childErrors)) {
            throw new ValidationError(bundle, errors, childErrors);
        }
    }

    private void checkChildren(boolean forUpdate) {
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(bundle.getBundleClass());
        ListMultimap<String, Bundle> relations = bundle.getRelations();
        for (String relation : relations.keySet()) {
            if (dependents.containsKey(relation)) {
                for (Bundle child : relations.get(relation)) {
                    try {
                        BundleValidator validator = BundleValidatorFactory.getInstance(manager, child);
                        if (forUpdate)
                            validator.validateTreeForUpdate();
                        else
                            validator.validateTree();
                    } catch (BundleError e) {
                        childErrors.put(relation, e);
                    }
                }
            }
        }
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

    /**
     * Search a tree of errors and determine if there's anything in it except for null values, which have to be there to
     * maintain item ordering.
     *
     * @param errors
     * @return
     */
    private boolean hasNestedErrors(ListMultimap<String, BundleError> errors) {
        for (BundleError e : errors.values()) {
            if (e != null) {
                return true;
            }
        }
        return false;
    }
}
