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


import java.util.Optional;

/**
 * Class holding information about a create-or-update job. The item
 * will either have been created, been updated, or remained unchanged.
 */
public final class Mutation<T> {
    private final T node;
    private final MutationState state;
    private final Optional<Bundle> prior;

    public Mutation(T node, MutationState state, Optional<Bundle> prior) {
        this.node = node;
        this.state = state;
        this.prior = prior;
    }

    public Mutation(T node, MutationState state) {
        this(node, state, Optional.empty());
    }

    public Mutation(T node, MutationState state, Bundle bundle) {
        this(node, state, Optional.ofNullable(bundle));
    }

    public static <T> Mutation<T> created(T item) {
        return new Mutation<>(item, MutationState.CREATED);
    }

    public static <T> Mutation<T> updated(T item) {
        return new Mutation<>(item, MutationState.UPDATED);
    }

    public static <T> Mutation<T> unchanged(T item) {
        return new Mutation<>(item, MutationState.UNCHANGED);
    }

    public T getNode() {
        return node;
    }

    public Optional<Bundle> getPrior() {
        return this.prior;
    }

    public MutationState getState() {
        return state;
    }

    public boolean hasChanged() {
        return state != MutationState.UNCHANGED;
    }

    public boolean created() {
        return state == MutationState.CREATED;
    }

    public boolean updated() {
        return state == MutationState.UPDATED;
    }
}
