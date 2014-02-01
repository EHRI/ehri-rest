package eu.ehri.project.persistence;

import com.google.common.base.Optional;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * Class responsible for validating bundles.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class BundleValidator {

    private final GraphManager manager;
    private final Iterable<String> scopes;

    public BundleValidator(GraphManager manager, Iterable<String> scopes) {
        this.manager = manager;
        this.scopes = Optional.fromNullable(scopes).or(Lists.<String>newArrayList());
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for creating in the graph.
     *
     * @return A new bundle with generated IDs.
     * @throws ValidationError
     */
    public Bundle validateForCreate(final Bundle bundle) throws ValidationError {
        validateData(bundle);
        Bundle withIds = bundle.generateIds(scopes);
        ErrorSet createErrors = validateTreeForCreate(withIds);
        if (!createErrors.isEmpty()) {
            throw new ValidationError(bundle, createErrors);
        }
        return withIds;
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for updating in the graph.
     * 
     *
     * @return A new bundle with generated IDs.
     * @throws ValidationError
     */
    public Bundle validateForUpdate(final Bundle bundle) throws ValidationError {
        validateData(bundle);
        Bundle withIds = bundle.generateIds(scopes);
        ErrorSet updateErrors = validateTreeForUpdate(withIds);
        if (!updateErrors.isEmpty()) {
            throw new ValidationError(bundle, updateErrors);
        }
        return withIds;
    }

    /**
     * Validate bundle fields. Mandatory fields must be present and non-empty
     * and entity type annotations must be present in the bundle's entity type class.
     *
     * @param bundle a Bundle to validate
     * @throws ValidationError if any errors were found during validation of the bundle
     */
    private void validateData(final Bundle bundle) throws ValidationError {
        ErrorSet es = validateTreeData(bundle);
        if (!es.isEmpty()) {
            throw new ValidationError(bundle, es);
        }
    }

    private static enum ValidationType {
        data, create, update
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for updating in the graph.
     *
     * @param bundle the Bundle to validate
     * @return errors an ErrorSet that is empty when no errors were found, 
     * 		or containing found errors
     */
    private ErrorSet validateTreeForUpdate(final Bundle bundle) {
        ErrorSet.Builder builder = new ErrorSet.Builder();
        if (bundle.getId() == null)
            builder.addError(Bundle.ID_KEY, Messages
                    .getString("BundleValidator.missingIdForUpdate")); //$NON-NLS-1$
        checkUniquenessOnUpdate(bundle, builder);
        checkChildren(bundle, builder, ValidationType.update);
        return builder.build();
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for creating in the graph.
     *
     * @param bundle the Bundle to validate
     * @return errors an ErrorSet that is empty when no errors were found, 
     * 		or containing found errors
     */
    private ErrorSet validateTreeForCreate(final Bundle bundle) {
        ErrorSet.Builder builder = new ErrorSet.Builder();
        checkIntegrity(bundle, builder);
        checkUniqueness(bundle, builder);
        checkChildren(bundle, builder, ValidationType.create);
        return builder.build();
    }

    /**
     * Validate bundle fields. Mandatory fields must be present and non-empty
     * and entity type annotations must be present in the bundle's entity type class.
     *
     * @return errors an ErrorSet that may contain errors for missing/empty mandatory fields
     * 		and missing entity types
     */
    private ErrorSet validateTreeData(final Bundle bundle) {
        ErrorSet.Builder builder = new ErrorSet.Builder();
        checkFields(bundle, builder);
        checkEntityType(bundle, builder);
        checkChildren(bundle, builder, ValidationType.data);
        return builder.build();
    }

    private void checkChildren(final Bundle bundle,
            final ErrorSet.Builder builder, ValidationType type) {
        for (Map.Entry<String, Bundle> entry : bundle.getDependentRelations().entries()) {
            switch (type) {
                case data:
                    builder.addRelation(entry.getKey(), validateTreeData(entry.getValue()));
                    break;
                case create:
                    builder.addRelation(entry.getKey(), validateTreeForCreate(entry.getValue()));
                    break;
                case update:
                    builder.addRelation(entry.getKey(), validateTreeForUpdate(entry.getValue()));
                    break;
            }
        }
    }

    /**
     * Check a bundle's mandatory fields are present and not empty. Add errors to the builder's ErrorSet.
     */
    private static void checkFields(final Bundle bundle, final ErrorSet.Builder builder) {
        for (String key : ClassUtils.getMandatoryPropertyKeys(bundle.getBundleClass())) {
            checkField(bundle, builder, key);
        }
    }

    /**
     * Check the data holds a given field and that the field is not empty.
     *
     * @param name The field name
     */
    private static void checkField(final Bundle bundle, final ErrorSet.Builder builder, String name) {
        if (!bundle.getData().containsKey(name)) {
            builder.addError(name, Messages.getString("BundleValidator.missingField"));
        } else {
            Object value = bundle.getData().get(name);
            if (value == null) {
                builder.addError(name, Messages.getString("BundleValidator.emptyField"));
            } else if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    builder.addError(name, Messages.getString("BundleValidator.emptyField"));
                }
            }
        }
    }

    /**
     * Check that the entity type annotation is present in the bundle's class.
     */
    private static void checkEntityType(final Bundle bundle, final ErrorSet.Builder builder) {
        EntityType annotation = bundle.getBundleClass().getAnnotation(EntityType.class);
        if (annotation == null) {
            builder.addError(Bundle.TYPE_KEY, MessageFormat.format(Messages
                    .getString("BundleValidator.missingTypeAnnotation"), //$NON-NLS-1$
                    bundle.getBundleClass().getName()));
        }
    }

    /**
     * Check uniqueness constraints for a bundle's fields. ID must be present and not existing in
     * the graph.
     */
    private void checkIntegrity(final Bundle bundle, final ErrorSet.Builder builder) {
        if (bundle.getId() == null) {
            builder.addError("id", MessageFormat.format(Messages.getString("BundleValidator.missingIdForCreate"),
                    bundle.getId()));
        }
        if (manager.exists(bundle.getId())) {
            ListMultimap<String, String> idErrors = bundle
                    .getType().getIdgen().handleIdCollision(scopes, bundle);
            for (Map.Entry<String, String> entry : idErrors.entries()) {
                builder.addError(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Check uniqueness constraints for a bundle's fields.
     */
    private void checkUniqueness(final Bundle bundle, final ErrorSet.Builder builder) {
        for (String ukey : bundle.getUniquePropertyKeys()) {
            Object uval = bundle.getDataValue(ukey);
            if (uval != null) {
                CloseableIterable<Vertex> vertices = manager.getVertices(ukey, uval, bundle.getType());
                try {
                    if (vertices.iterator().hasNext()) {
                        builder.addError(ukey, MessageFormat.format(Messages
                                .getString("BundleValidator.uniquenessError"), uval));
                    }
                } finally {
                    vertices.close();
                }
            }
        }
    }

    /**
     * Check uniqueness constraints for a bundle's fields: add errors for node references whose referent is 
     * not already in the graph.
     */
    private void checkUniquenessOnUpdate(final Bundle bundle, final ErrorSet.Builder builder) {
        for (String ukey : bundle.getUniquePropertyKeys()) {
            Object uval = bundle.getDataValue(ukey);
            if (uval != null) {
                CloseableIterable<Vertex> vertices = manager.getVertices(ukey, uval, bundle.getType());
                try {
                    if (vertices.iterator().hasNext()) {
                        Vertex v = vertices.iterator().next();
                        // If it's the same vertex, we don't have a problem...
                        if (!manager.getId(v).equals(bundle.getId())) {
                            builder.addError(ukey, MessageFormat.format(Messages
                                    .getString("BundleValidator.uniquenessError"), uval));

                        }
                    }
                } finally {
                    vertices.close();
                }
            }
        }
    }
}
