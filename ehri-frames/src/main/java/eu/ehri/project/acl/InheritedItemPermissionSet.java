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

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * A user's permissions on an individual item, including
 * those inherited from groups to which they belong.
 *
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
         * @param accessorId    The primary accessor's ID
         * @param permissionSet The primary accessor's own global permissions
         */
        public Builder(String accessorId, List<PermissionType> permissionSet) {
            AccessorPermissions<List<PermissionType>> permissions
                    = new AccessorPermissions<>(accessorId, permissionSet);
            perms.add(permissions);
        }

        /**
         * Add an accessor from whom the user inherits permissions.
         *
         * @param accessorId    The accessor's ID
         * @param permissionSet The accessor's permissions
         * @return The builder
         */
        public Builder withInheritedPermissions(String accessorId, List<PermissionType> permissionSet) {
            perms.add(new AccessorPermissions<>(accessorId, permissionSet));
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
     * @return A list of accessor id -&gt; permission mappings
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
