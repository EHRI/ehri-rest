package eu.ehri.project.acl;

import com.google.common.collect.Sets;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

import java.util.Set;

/**
 * User: mike
 *
 * Convenience wrapper for the permission set data structure, which
 * looks like:
 *
 *  {
 *      contentType -> [perms...],
 *      ...
 *  }
 */
public final class ItemPermissionSet {

    private final Set<PermissionType> set;

    @JsonCreator
    public static ItemPermissionSet from(Set<PermissionType> set) {
        return new ItemPermissionSet(set);
    }

    private ItemPermissionSet(Set<PermissionType> permissionSet) {
        set = Sets.immutableEnumSet(permissionSet);
    }

    public boolean has(PermissionType permissionType) {
        return set.contains(permissionType);
    }

    @JsonValue
    public Set<PermissionType> asSet() {
        return set;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItemPermissionSet that = (ItemPermissionSet) o;

        return set.equals(that.set);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    @Override
    public String toString() {
        return "<ItemPermissions: " + set.toString() + ">";
    }
}
