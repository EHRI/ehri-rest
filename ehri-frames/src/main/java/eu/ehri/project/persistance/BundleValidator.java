package eu.ehri.project.persistance;

import com.google.common.base.Optional;
import com.google.common.collect.ListMultimap;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.ClassUtils;
import java.text.MessageFormat;
import java.util.Map;

/**
 * Class responsible for validating bundles.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public final class BundleValidator {

    private final GraphManager manager;
    private final PermissionScope scope;

    public BundleValidator(GraphManager manager, PermissionScope scope) {
        this.manager = manager;
        this.scope = Optional.fromNullable(scope).or(SystemScope.getInstance());
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
        Bundle withIds = bundle.generateIds(scope);
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
     * @return A new bundle with generated IDs.
     * @throws ValidationError
     */
    public Bundle validateForUpdate(final Bundle bundle) throws ValidationError {
        validateData(bundle);
        Bundle withIds = bundle.generateIds(scope);
        ErrorSet updateErrors = validateTreeForUpdate(withIds);
        if (!updateErrors.isEmpty()) {
            throw new ValidationError(bundle, updateErrors);
        }
        return withIds;
    }

    /**
     * Validate bundle fields.
     *
     * @throws ValidationError
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
     * @return errors
     */
    private ErrorSet validateTreeForUpdate(final Bundle bundle) {
        ErrorSet.Builder builder = new ErrorSet.Builder();
        if (bundle.getId() == null)
            builder.addError(Bundle.ID_KEY, eu.ehri.project.persistance.Messages
                    .getString("BundleValidator.missingIdForUpdate")); //$NON-NLS-1$
        checkUniquenessOnUpdate(bundle, builder);
        checkChildren(bundle, builder, ValidationType.update);
        return builder.build();
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for updating in the graph.
     *
     * @return errors
     */
    private ErrorSet validateTreeForCreate(final Bundle bundle) {
        ErrorSet.Builder builder = new ErrorSet.Builder();
        checkIntegrity(bundle, builder);
        checkUniqueness(bundle, builder);
        checkChildren(bundle, builder, ValidationType.create);
        return builder.build();
    }

    /**
     * Validate bundle fields.
     *
     * @return errors
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
        Map<String, Direction> dependents = ClassUtils
                .getDependentRelations(bundle.getBundleClass());
        ListMultimap<String, Bundle> relations = bundle.getRelations();
        for (String relation : relations.keySet()) {
            if (dependents.containsKey(relation)) {
                for (Bundle child : relations.get(relation)) {
                    switch (type) {
                        case data:
                            builder.addRelation(relation, validateTreeData(child));
                            break;
                        case create:
                            builder.addRelation(relation, validateTreeForCreate(child));
                            break;
                        case update:
                            builder.addRelation(relation, validateTreeForUpdate(child));
                            break;
                    }
                }
            }
        }
    }

    /**
     * Check a bundle's fields validate.
     */
    private static void checkFields(final Bundle bundle, final ErrorSet.Builder builder) {
        for (String key : ClassUtils.getMandatoryPropertyKeys(bundle.getBundleClass())) {
            checkField(bundle, builder, key);
        }
    }

    /**
     * Check the data holds a given field, accounting for the
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
     * Check entity type annotation.
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
     * Check uniqueness constrains for a bundle's fields.
     */
    private void checkIntegrity(final Bundle bundle, final ErrorSet.Builder builder) {
        if (bundle.getId() == null) {
            builder.addError("id", MessageFormat.format(Messages.getString("BundleValidator.missingIdForCreate"),
                    bundle.getId()));
        }
        if (manager.exists(bundle.getId())) {
            ListMultimap<String, String> idErrors = bundle
                    .getType().getIdgen()
                    .handleIdCollision(bundle.getType(), scope, bundle);
            for (Map.Entry<String, String> entry : idErrors.entries()) {
                builder.addError(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Check uniqueness constrains for a bundle's fields.
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
     * Check uniqueness constrains for a bundle's fields.
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
