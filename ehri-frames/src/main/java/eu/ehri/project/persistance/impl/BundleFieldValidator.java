package eu.ehri.project.persistance.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;
import eu.ehri.project.persistance.Bundle;
import eu.ehri.project.persistance.ErrorSet;

import java.text.MessageFormat;
import java.util.List;
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
public final class BundleFieldValidator {

    private final GraphManager manager;
    private final PermissionScope scope;

    public BundleFieldValidator(GraphManager manager, PermissionScope scope) {
        this.manager = manager;
        this.scope = scope;
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for updating in the graph.
     *
     * @return errors
     *
     */
    public void validateForUpdate(final Bundle bundle) throws ValidationError {
        ErrorSet es = validateTreeForUpdate(bundle);
        if (!es.isEmpty()) {
            throw new ValidationError(bundle, es);
        }
    }

    /**
     * Validate bundle fields.
     *
     * @return errors
     */
    public void validate(final Bundle bundle) throws ValidationError {
        ErrorSet es = validateTree(bundle);
        if (!es.isEmpty()) {
            throw new ValidationError(bundle, es);
        }
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for updating in the graph.
     * 
     * @return errors
     * 
     */
    private ErrorSet validateTreeForUpdate(final Bundle bundle) {
        ErrorSet.Builder builder = new ErrorSet.Builder();
        if (bundle.getId() == null)
            builder.addError(Bundle.ID_KEY, Messages
                    .getString("BundleFieldValidator.missingIdForUpdate")); //$NON-NLS-1$
        builder.addErrors(checkFields(bundle));
        builder.addErrors(checkEntityType(bundle));
        builder.addErrors(checkUniquenessOnUpdate(bundle));
        builder.addRelations(checkChildren(bundle, true));
        return builder.build();
    }

    /**
     * Validate bundle fields.
     * 
     * @return errors
     */
    private ErrorSet validateTree(final Bundle bundle) {
        ErrorSet.Builder builder = new ErrorSet.Builder();
        builder.addErrors(checkFields(bundle));
        builder.addErrors(checkEntityType(bundle));
        builder.addErrors(checkIntegrity(bundle));
        builder.addErrors(checkUniqueness(bundle));
        builder.addRelations(checkChildren(bundle, false));
        return builder.build();
    }

    private ListMultimap<String,ErrorSet> checkChildren(final Bundle bundle, boolean forUpdate) {
        ListMultimap<String,ErrorSet> errors = ArrayListMultimap.create();
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(bundle.getBundleClass());
        ListMultimap<String, Bundle> relations = bundle.getRelations();
        for (String relation : relations.keySet()) {
            if (dependents.containsKey(relation)) {
                for (Bundle child : relations.get(relation)) {
                    if (forUpdate) {
                        errors.put(relation, validateTreeForUpdate(child));
                    } else {
                        errors.put(relation, validateTree(child));
                    }
                }
            }
        }
        return errors;
    }

    /**
     * Check a bundle's fields validate.
     */
    private static ListMultimap<String,String> checkFields(final Bundle bundle) {
        ListMultimap<String,String> errors = ArrayListMultimap.create();
        for (String key : ClassUtils.getMandatoryPropertyKeys(bundle.getBundleClass())) {
            errors.putAll(key, checkField(bundle, key));
        }
        return errors;
    }

    /**
     * Check the data holds a given field, accounting for the
     * 
     * @param name
     * @return  A list of errors for the given field.
     */
    private static List<String> checkField(final Bundle bundle, String name) {
        List<String> errors = Lists.newArrayList();
        if (!bundle.getData().containsKey(name)) {
            errors.add(Messages.getString("BundleFieldValidator.missingField"));
        } else {
            Object value = bundle.getData().get(name);
            if (value == null) {
                errors.add(Messages.getString("BundleFieldValidator.emptyField"));
            } else if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    errors.add(Messages.getString("BundleFieldValidator.emptyField"));
                }
            }
        }
        return errors;
    }

    /**
     * Check entity type annotation.
     */
    private static ListMultimap<String,String> checkEntityType(final Bundle bundle) {
        ListMultimap<String,String> errors = ArrayListMultimap.create();
        EntityType annotation = bundle.getBundleClass().getAnnotation(EntityType.class);
        if (annotation == null) {
            errors.put(Bundle.TYPE_KEY, MessageFormat.format(Messages
                    .getString("BundleFieldValidator.missingTypeAnnotation"), //$NON-NLS-1$
                    bundle.getBundleClass().getName()));
        }
        return errors;
    }

    /**
     * Check uniqueness constrains for a bundle's fields.
     */
    private ListMultimap<String,String> checkIntegrity(final Bundle bundle) {
        ListMultimap<String,String> errors = ArrayListMultimap.create();
        if (bundle.getId() == null) {
            errors.put("id", MessageFormat.format(Messages.getString("BundleFieldValidator.missingIdForCreate"),
                    bundle.getId()));
        }
        if (manager.exists(bundle.getId())) {
            ListMultimap<String, String> idErrors = bundle
                    .getType().getIdgen()
                    .handleIdCollision(bundle.getType(), scope, bundle);
            for (Map.Entry<String,String> entry :idErrors.entries()) {
                errors.put(entry.getKey(), entry.getValue());
            }
        }
        return errors;
    }

    /**
     * Check uniqueness constrains for a bundle's fields.
     */
    private ListMultimap<String,String> checkUniqueness(final Bundle bundle) {
        ListMultimap<String,String> errors = ArrayListMultimap.create();
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
        return errors;
    }

    /**
     * Check uniqueness constrains for a bundle's fields.
     */
    private ListMultimap<String,String> checkUniquenessOnUpdate(final Bundle bundle) {
        ListMultimap<String,String> errors = ArrayListMultimap.create();
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
        return errors;
    }
}
