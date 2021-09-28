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

package eu.ehri.project.exceptions;

import com.google.common.base.Preconditions;
import eu.ehri.project.models.EntityClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Represents a failure to find an item in the graph based
 * on its ID value, an ID with an Entity Class, or an arbitrary
 * key/value pair.
 */
public class ItemNotFound extends Exception {
    private static final long serialVersionUID = -3562833443079995695L;

    private final String id;
    private final EntityClass cls;
    private final String deleted;

    public ItemNotFound(String id) {
        super(String.format("Item with id '%s' not found", id));
        Preconditions.checkNotNull(id);
        this.id = id;
        this.cls = null;
        this.deleted = null;
    }

    public ItemNotFound(@Nonnull String id, @Nonnull EntityClass cls) {
        this(id, cls, null);
    }

    public ItemNotFound(String id, @Nullable EntityClass cls, @Nullable String deletedAt) {
        super(String.format("Item with id '%s' and type '%s' not found", id, cls != null ? cls.getName() : null));
        this.id = id;
        this.cls = cls;
        this.deleted = deletedAt;
    }

    public String getId() {
        return id;
    }

    public Optional<EntityClass> getEntityClass() {
        return Optional.ofNullable(cls);
    }

    public Optional<String> getDeletedAt() { return Optional.ofNullable(deleted); }

    public ItemNotFound withDeletedAt(String deletedAt) {
        Preconditions.checkNotNull(deletedAt);
        return new ItemNotFound(id, cls, deletedAt);
    }
}
