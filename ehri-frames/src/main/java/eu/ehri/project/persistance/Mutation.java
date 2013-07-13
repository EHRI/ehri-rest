package eu.ehri.project.persistance;

/**
 * Class holding information about a create-or-update job. The item
 * will either have been created, been updated, or remained unchanged.
 */
public final class Mutation<T> {
    private final T node;
    private final MutationState state;
    public Mutation(T node, MutationState state) {
        this.node = node;
        this.state = state;
    }
    public T getNode() {
        return node;
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
