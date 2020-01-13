/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.persistence;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.utils.ClassUtils;

import java.text.MessageFormat;
import java.util.*;

/**
 * Class responsible for validating bundles.
 */
public final class BundleValidator {

    private final GraphManager manager;
    private final List<String> scopes;

    public BundleValidator(GraphManager manager, Collection<String> scopes) {
        this.manager = manager;
        this.scopes = Lists.newArrayList(Optional.ofNullable(scopes)
                .orElse(Lists.newArrayList()));
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for creating in the graph.
     *
     * @return A new bundle with generated IDs.
     */
    Bundle validateForCreate(Bundle bundle) throws ValidationError {
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
     * @return A new bundle with generated IDs.
     */
    Bundle validateForUpdate(Bundle bundle) throws ValidationError {
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
    private void validateData(Bundle bundle) throws ValidationError {
        ErrorSet es = validateTreeData(bundle);
        if (!es.isEmpty()) {
            throw new ValidationError(bundle, es);
        }
    }

    private enum ValidationType {
        data, create, update
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for updating in the graph.
     *
     * @param bundle the Bundle to validate
     * @return errors an ErrorSet that is empty when no errors were found,
     * or containing found errors
     */
    private ErrorSet validateTreeForUpdate(Bundle bundle) {
        ErrorSet.Builder builder = new ErrorSet.Builder();
        if (bundle.getId() == null)
            builder.addError(Bundle.ID_KEY, Messages
                    .getString("BundleValidator.missingIdForUpdate")); //$NON-NLS-1$
        checkUniquenessOnUpdate(bundle, builder);
        checkChildren(bundle, builder, ValidationType.update);
        checkDuplicateIds(bundle, builder);
        return builder.build();
    }

    /**
     * Validate the data in the bundle, according to the target class, and
     * ensure it is fit for creating in the graph.
     *
     * @param bundle the Bundle to validate
     * @return errors an ErrorSet that is empty when no errors were found,
     * or containing found errors
     */
    private ErrorSet validateTreeForCreate(Bundle bundle) {
        ErrorSet.Builder builder = new ErrorSet.Builder();
        checkIntegrity(bundle, builder);
        checkUniqueness(bundle, builder);
        checkChildren(bundle, builder, ValidationType.create);
        checkDuplicateIds(bundle, builder);
        return builder.build();
    }

    /**
     * Validate bundle fields. Mandatory fields must be present and non-empty
     * and entity type annotations must be present in the bundle's entity type class.
     *
     * @return errors an ErrorSet that may contain errors for missing/empty mandatory fields
     * and missing entity types
     */
    private ErrorSet validateTreeData(Bundle bundle) {
        ErrorSet.Builder builder = new ErrorSet.Builder();
        checkFields(bundle, builder);
        checkEntityType(bundle, builder);
        checkChildren(bundle, builder, ValidationType.data);
        return builder.build();
    }

    private void checkChildren(Bundle bundle,
            ErrorSet.Builder builder, ValidationType type) {
        final Set<String> ids = Sets.newHashSet();
        for (Map.Entry<String, Bundle> entry : bundle.getDependentRelations().entries()) {
            Bundle child = entry.getValue();
            ErrorSet errorSet = type == ValidationType.data
                    ? validateTreeData(child)
                    : (type == ValidationType.create
                        ? validateTreeForCreate(child)
                        : validateTreeForUpdate(child));
            if (errorSet.isEmpty() && child.getId() != null) {
                if (ids.contains(child.getId())) {
                    ListMultimap<String, String> errs = child.getType().getIdGen()
                            .handleIdCollision(Lists.newArrayList(bundle.getId()), child);
                    for (Map.Entry<String, String> err : errs.entries()) {
                        errorSet = errorSet.withDataValue(err.getKey(), err.getValue());
                    }
                } else {
                    ids.add(child.getId());
                }
            }
            builder.addRelation(entry.getKey(), errorSet);
        }
    }

    private static void checkDuplicateIds(Bundle bundle, ErrorSet.Builder builder) {
        final Set<String> ids = Sets.newHashSet();
        bundle.dependentsOnly().forEach(b -> {
            if (ids.contains(b.getId())) {
                builder.addError(Bundle.ID_KEY, MessageFormat.format(
                        Messages.getString("BundleValidator.duplicateId"), b.getId()));
            }
            ids.add(b.getId());
        });
    }

    /**
     * Check a bundle's mandatory fields are present and not empty. Add errors to the builder's ErrorSet.
     */
    private static void checkFields(Bundle bundle, ErrorSet.Builder builder) {
        for (String key : ClassUtils.getMandatoryPropertyKeys(bundle.getBundleJavaClass())) {
            checkField(bundle, builder, key);
        }
        Map<String, Set<String>> enumPropertyKeys = ClassUtils.getEnumPropertyKeys(bundle.getBundleJavaClass());
        for (Map.Entry<String, Set<String>> entry : enumPropertyKeys.entrySet()) {
            checkValueInRange(bundle, builder, entry.getKey(), entry.getValue());
        }
    }

    private static void checkValueInRange(Bundle bundle, ErrorSet.Builder builder, String key, Collection<String> values) {
        Object dataValue = bundle.getDataValue(key);
        if (dataValue != null) {
            if (!values.contains(dataValue.toString())) {
                builder.addError(key,
                        MessageFormat.format(Messages.getString("BundleValidator.invalidFieldValue"), values, dataValue));
            }
        }
    }

    /**
     * Check the data holds a given field and that the field is not empty.
     *
     * @param name The field name
     */
    private static void checkField(Bundle bundle, ErrorSet.Builder builder, String name) {
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
    private static void checkEntityType(Bundle bundle, ErrorSet.Builder builder) {
        EntityType annotation = bundle.getBundleJavaClass().getAnnotation(EntityType.class);
        if (annotation == null) {
            builder.addError(Bundle.TYPE_KEY,
                    MessageFormat.format(Messages.getString("BundleValidator.missingTypeAnnotation"),
                    bundle.getBundleJavaClass().getName()));
        }
    }

    /**
     * Check uniqueness constraints for a bundle's fields. ID must be present and not existing in
     * the graph.
     */
    private void checkIntegrity(Bundle bundle, ErrorSet.Builder builder) {
        if (bundle.getId() == null) {
            builder.addError(Bundle.ID_KEY,
                    MessageFormat.format(Messages.getString("BundleValidator.missingIdForCreate"),
                    bundle.getId()));
        }
        if (manager.exists(bundle.getId())) {
            ListMultimap<String, String> idErrors = bundle
                    .getType().getIdGen().handleIdCollision(scopes, bundle);
            for (Map.Entry<String, String> entry : idErrors.entries()) {
                builder.addError(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Check uniqueness constraints for a bundle's fields.
     */
    private void checkUniqueness(Bundle bundle, ErrorSet.Builder builder) {
        for (String ukey : bundle.getUniquePropertyKeys()) {
            Object uval = bundle.getDataValue(ukey);
            if (uval != null) {
                try (CloseableIterable<Vertex> vertices = manager.getVertices(ukey, uval, bundle.getType())) {
                    if (vertices.iterator().hasNext()) {
                        builder.addError(ukey, MessageFormat.format(Messages
                                .getString("BundleValidator.uniquenessError"), uval));
                    }
                }
            }
        }
    }

    /**
     * Check uniqueness constraints for a bundle's fields: add errors for node references whose referent is
     * not already in the graph.
     */
    private void checkUniquenessOnUpdate(Bundle bundle, ErrorSet.Builder builder) {
        for (String ukey : bundle.getUniquePropertyKeys()) {
            Object uval = bundle.getDataValue(ukey);
            if (uval != null) {
                try (CloseableIterable<Vertex> vertices = manager.getVertices(ukey, uval, bundle.getType())) {
                    if (vertices.iterator().hasNext()) {
                        Vertex v = vertices.iterator().next();
                        // If it's the same vertex, we don't have a problem...
                        if (!manager.getId(v).equals(bundle.getId())) {
                            builder.addError(ukey, MessageFormat.format(Messages
                                    .getString("BundleValidator.uniquenessError"), uval));

                        }
                    }
                }
            }
        }
    }
}
