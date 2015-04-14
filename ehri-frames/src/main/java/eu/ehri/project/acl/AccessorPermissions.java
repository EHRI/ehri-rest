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

import com.google.common.collect.ImmutableMap;
import eu.ehri.project.models.base.Accessor;
import org.codehaus.jackson.annotate.JsonValue;

import java.util.Map;

/**
 * A pairing of an accessor and their non-inherited permissions.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
class AccessorPermissions<T> {
    final String accessorId;
    final T permissionSet;

    public AccessorPermissions(String accessorId, T permissionSet) {
        this.accessorId = accessorId;
        this.permissionSet = permissionSet;
    }

    @JsonValue
    public Map<String, T> asMap() {
        return ImmutableMap.of(accessorId, permissionSet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccessorPermissions that = (AccessorPermissions) o;

        return accessorId.equals(that.accessorId)
                && permissionSet.equals(that.permissionSet);

    }

    @Override
    public int hashCode() {
        int result = accessorId.hashCode();
        result = 31 * result + permissionSet.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "<" + accessorId + " " + permissionSet + ">";
    }
}
