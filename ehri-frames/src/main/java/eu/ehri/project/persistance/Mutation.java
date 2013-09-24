package eu.ehri.project.persistance;

import com.google.common.base.Optional;

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
        this(node, state, Optional.<Bundle>absent());
    }

    public Mutation(T node, MutationState state, Bundle bundle) {
        this(node, state, Optional.fromNullable(bundle));
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

    public boolean unchanged() {
        return state == MutationState.UNCHANGED;
    }

    public boolean created() {
        return state == MutationState.CREATED;
    }

    public boolean updated() {
        return state == MutationState.UPDATED;
    }
}
