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

package eu.ehri.project.acl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Convenience wrapper for the permission set data structure, which
 * looks like:
 * <pre>
 *     <code>
 *  {
 *      contentType -&gt; [perms...],
 *      ...
 *  }
 *     </code>
 * </pre>
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
        return "<ItemPermissions: " + set + ">";
    }
}
