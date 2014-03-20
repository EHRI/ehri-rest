package eu.ehri.project.acl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import eu.ehri.project.models.base.Accessor;
import org.codehaus.jackson.annotate.JsonValue;

import java.util.List;
import java.util.Map;

/**
 * A user's complete set of permissions, including
 * those inherited from groups to which they belong.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class InheritedGlobalPermissionSet {

    /**
     * A pairing of an accessor and their non-inherited permissions.
     */
    private static class AccessorPermissions {
        final Accessor accessor;
        final GlobalPermissionSet permissionSet;

        public AccessorPermissions(Accessor accessor, GlobalPermissionSet permissionSet) {
            this.accessor = accessor;
            this.permissionSet = permissionSet;
        }

        @JsonValue
        public Map<String, GlobalPermissionSet> asMap() {
            return ImmutableMap.of(accessor.getId(), permissionSet);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AccessorPermissions that = (AccessorPermissions) o;

            return accessor.equals(that.accessor)
                    && permissionSet.equals(that.permissionSet);

        }

        @Override
        public int hashCode() {
            int result = accessor.hashCode();
            result = 31 * result + permissionSet.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "<" + accessor.getId() + " " + permissionSet + ">";
        }
    }

    /**
     * Builder class for InheritedGlobalPermissionSets.
     */
    public static class Builder {
        private List<AccessorPermissions> perms;

        /**
         * Create a new builder with the primary (subject) accessor.
         *
         * @param accessor      The primary accessor
         * @param permissionSet The primary accessor's own global permissions
         */
        public Builder(Accessor accessor, GlobalPermissionSet permissionSet) {
            perms = Lists.newArrayList(new AccessorPermissions(accessor, permissionSet));
        }

        /**
         * Add an accessor from whom the user inherits permissions.
         *
         * @param accessor      The accessor
         * @param permissionSet The accessor's permissions
         * @return The builder
         */
        public Builder withInheritedPermissions(Accessor accessor, GlobalPermissionSet permissionSet) {
            perms.add(new AccessorPermissions(accessor, permissionSet));
            return this;
        }

        /**
         * Construct the InheritedGlobalPermissionSet from the builder.
         *
         * @return A new InheritedGlobalPermissionSet
         */
        public InheritedGlobalPermissionSet build() {
            return new InheritedGlobalPermissionSet(perms);
        }
    }

    private final List<AccessorPermissions> permissionsList;

    private InheritedGlobalPermissionSet(List<AccessorPermissions> permissionsList) {
        this.permissionsList = permissionsList;
    }

    /**
     * Test if this inherited permission set contains a given permission.
     *
     * @param contentType    The content type
     * @param permissionType The permission type
     * @return Whether or not the permission is present
     */
    public boolean has(ContentTypes contentType, PermissionType permissionType) {
        for (AccessorPermissions accessorPermissions : permissionsList) {
            if (accessorPermissions.permissionSet.has(contentType, permissionType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InheritedGlobalPermissionSet that = (InheritedGlobalPermissionSet) o;

        return permissionsList.equals(that.permissionsList);

    }

    @Override
    public int hashCode() {
        return permissionsList.hashCode();
    }

    /**
     * Serialize the InheritedGlobalPermissionSet to a
     * list containing a mappings of accessor ID to permissions.
     *
     * @return A list of accessor id -> permission mappings
     */
    @JsonValue
    public List<Map<String, GlobalPermissionSet>> serialize() {
        List<Map<String, GlobalPermissionSet>> tmp = Lists.newArrayList();
        for (AccessorPermissions accessorPermissions : permissionsList) {
            tmp.add(accessorPermissions.asMap());
        }
        return tmp;
    }

    @Override
    public String toString() {
        return permissionsList.toString();
    }
}
