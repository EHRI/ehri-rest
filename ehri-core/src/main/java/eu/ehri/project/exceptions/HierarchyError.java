package eu.ehri.project.exceptions;

public class HierarchyError extends Exception {
    private final String id;
    private final int childCount;

    public HierarchyError(String parentId, int childCount) {
        super(String.format("Item '%s' has %d child items", parentId, childCount));
        this.id = parentId;
        this.childCount = childCount;
    }

    public String id() {
        return id;
    }

    public int childCount() {
        return childCount;
    }
}
