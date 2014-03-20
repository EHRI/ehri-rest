package eu.ehri.project.acl;

import com.google.common.collect.Lists;
import eu.ehri.project.models.base.Accessor;
import org.codehaus.jackson.annotate.JsonValue;

import java.util.List;
import java.util.Map;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class InheritedItemPermissionSet {
    /**
     * Builder class for InheritedItemPermissionSets.
     */
    public static class Builder {
        private final List<AccessorPermissions<List<PermissionType>>> perms
                = Lists.newArrayList();

        /**
         * Create a new builder with the primary (subject) accessor.
         *
         * @param accessor      The primary accessor
         * @param permissionSet The primary accessor's own global permissions
         */
        public Builder(Accessor accessor, List<PermissionType> permissionSet) {
            AccessorPermissions<List<PermissionType>> permissions
                    = new AccessorPermissions<List<PermissionType>>(accessor, permissionSet);
            perms.add(permissions);
        }

        /**
         * Add an accessor from whom the user inherits permissions.
         *
         * @param accessor      The accessor
         * @param permissionSet The accessor's permissions
         * @return The builder
         */
        public Builder withInheritedPermissions(Accessor accessor, List<PermissionType> permissionSet) {
            perms.add(new AccessorPermissions<List<PermissionType>>(accessor, permissionSet));
            return this;
        }

        /**
         * Construct the InheritedItemPermissionSet from the builder.
         *
         * @return A new InheritedItemPermissionSet
         */
        public InheritedItemPermissionSet build() {
            return new InheritedItemPermissionSet(perms);
        }
    }

    private final List<AccessorPermissions<List<PermissionType>>> permissionsList;

    private InheritedItemPermissionSet(List<AccessorPermissions<List<PermissionType>>> permissionsList) {
        this.permissionsList = permissionsList;
    }

    /**
     * Determine if the permission set contains a permission for the given item.
     *
     * @param permissionType The permission type
     * @return Whether or not the permission exists
     */
    public boolean has(PermissionType permissionType) {
        for (AccessorPermissions<List<PermissionType>> permissions : permissionsList) {
            if (permissions.permissionSet.contains(permissionType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Serialize the InheritedItemPermissionSet to a list
     * containing a mappings of accessor ID to permissions.
     *
     * @return A list of accessor id -> permission mappings
     */
    @JsonValue
    public List<Map<String, List<PermissionType>>> serialize() {
        List<Map<String, List<PermissionType>>> tmp = Lists.newArrayList();
        for (AccessorPermissions<List<PermissionType>> accessorPermissions : permissionsList) {
            tmp.add(accessorPermissions.asMap());
        }
        return tmp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InheritedItemPermissionSet that = (InheritedItemPermissionSet) o;

        return permissionsList.equals(that.permissionsList);

    }

    @Override
    public int hashCode() {
        return permissionsList.hashCode();
    }

    @Override
    public String toString() {
        return permissionsList.toString();
    }
}
