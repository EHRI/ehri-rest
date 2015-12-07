/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.acl;

import com.google.common.collect.Lists;
import com.fasterxml.jackson.annotation.JsonValue;

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
     * Builder class for InheritedGlobalPermissionSets.
     */
    public static class Builder {
        private final List<AccessorPermissions<GlobalPermissionSet>> perms = Lists.newArrayList();

        /**
         * Create a new builder with the primary (subject) accessor.
         *
         * @param accessorId      The primary accessor's ID
         * @param permissionSet The primary accessor's own global permissions
         */
        public Builder(String accessorId, GlobalPermissionSet permissionSet) {
            AccessorPermissions<GlobalPermissionSet> permissions
                    = new AccessorPermissions<>(accessorId, permissionSet);
            perms.add(permissions);
        }

        /**
         * Add an accessor from whom the user inherits permissions.
         *
         * @param accessorId    The accessor
         * @param permissionSet The accessor's permissions
         * @return The builder
         */
        public Builder withInheritedPermissions(String accessorId, GlobalPermissionSet permissionSet) {
            perms.add(new AccessorPermissions<>(accessorId, permissionSet));
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

    private final List<AccessorPermissions<GlobalPermissionSet>> permissionsList;

    private InheritedGlobalPermissionSet(List<AccessorPermissions<GlobalPermissionSet>> permissionsList) {
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
        for (AccessorPermissions<GlobalPermissionSet> accessorPermissions : permissionsList) {
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
     * @return A list of accessor id -&gt; permission mappings
     */
    @JsonValue
    public List<Map<String, GlobalPermissionSet>> serialize() {
        List<Map<String, GlobalPermissionSet>> tmp = Lists.newArrayList();
        for (AccessorPermissions<GlobalPermissionSet> accessorPermissions : permissionsList) {
            tmp.add(accessorPermissions.asMap());
        }
        return tmp;
    }

    @Override
    public String toString() {
        return permissionsList.toString();
    }
}
