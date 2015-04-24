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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

import java.util.Collection;
import java.util.Map;

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
public final class GlobalPermissionSet {

    public static class Builder {
        private final HashMultimap<ContentTypes,PermissionType> m;

        public Builder() {
            m = HashMultimap.create();
        }

        public Builder(Multimap<ContentTypes,PermissionType> initial) {
            m = HashMultimap.create(initial);
        }

        public Builder set(ContentTypes contentType, Collection<PermissionType> permissionTypes) {
            m.putAll(contentType, permissionTypes);
            return this;
        }

        public Builder set(ContentTypes contentType, PermissionType... permissionTypes) {
            m.putAll(contentType, Lists.newArrayList(permissionTypes));
            return this;
        }

        public GlobalPermissionSet build() {
            return new GlobalPermissionSet(m);
        }
    }

    private final HashMultimap<ContentTypes,PermissionType> matrix;

    @JsonCreator
    private GlobalPermissionSet(Multimap<ContentTypes,PermissionType> permissionMatrix) {
        matrix = HashMultimap.create(permissionMatrix);
    }

    private GlobalPermissionSet() {
        matrix = HashMultimap.create();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static GlobalPermissionSet empty() {
        return new GlobalPermissionSet();
    }

    public static GlobalPermissionSet from(Multimap<ContentTypes,PermissionType>  permissionMatrix) {
        return new GlobalPermissionSet(permissionMatrix);
    }

    public static GlobalPermissionSet from(Map<ContentTypes,Collection<PermissionType>>  permissionMatrix) {
        Multimap<ContentTypes,PermissionType> multimap = HashMultimap.create();
        for (Map.Entry<ContentTypes,Collection<PermissionType>> entry : permissionMatrix.entrySet()) {
            multimap.putAll(entry.getKey(), entry.getValue());
        }
        return from(multimap);
    }

    public boolean has(ContentTypes contentTypes, PermissionType permissionType) {
        return matrix.get(contentTypes).contains(permissionType);
    }

    public Collection<PermissionType> get(ContentTypes type) {
        return matrix.get(type);
    }

    public GlobalPermissionSet withPermission(ContentTypes contentType, PermissionType... permission) {
        return new Builder(matrix).set(contentType, permission).build();
    }

    @JsonValue
    public Map<ContentTypes,Collection<PermissionType>> asMap() {
        return matrix.asMap();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GlobalPermissionSet that = (GlobalPermissionSet) o;

        return matrix.equals(that.matrix);
    }

    @Override
    public int hashCode() {
        return matrix.hashCode();
    }

    @Override
    public String toString() {
        return "<GlobalPermissions: " + matrix + ">";
    }
}
