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

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

/**
 * An enum of permission types that can be assigned to
 * permission grants. Like the {@link ContentTypes} enum,
 * each permission value has an equivalent node in the grant
 * to which permission grants point.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public enum PermissionType {
    CREATE("create", 1)
    , UPDATE("update", 2)
    , DELETE("delete", 4)
    , ANNOTATE("annotate", 8)
    , OWNER("owner", 15) // Implies C,U,D,A
    , GRANT("grant", 16)

    , PROMOTE("promote", 32)
    // Reserved permission types
//    RESERVED2("reserved2", 32),
//    RESERVED3("reserved3", 64),
//    RESERVED4("reserved4", 128)
    ;

    
    private final String name;
    private final int mask;
    
    PermissionType(String name, int mask) {
        this.name = name;
        this.mask = mask;
    }

    /**
     * Fetch a textual representation of this permission.
     *
     * @return  name string
     */
    @JsonValue
    public String getName() {
        return name;
    }

    @Override public String toString() {
        return name;
    }

    /**
     * Return whether a given other permission is encompassed
     * by the current one.
     *
     * @param other another permission
     * @return if this permission is subordinate to the other
     */
    public boolean contains(PermissionType other) {
        return (mask & other.mask) == other.mask;
    }
    
    @JsonCreator
    public static PermissionType withName(String name) {
        for (PermissionType p : PermissionType.values()) {
            if (p.getName().equals(name))
                return p;
        }
        throw new IllegalArgumentException("Invalid permission type: " + name);
    }    
}
