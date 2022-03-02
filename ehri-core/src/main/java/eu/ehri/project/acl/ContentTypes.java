/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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
import eu.ehri.project.definitions.Entities;

/**
 * An enum of entity types that represent first-class
 * content types, meaning that permissions can be
 * granted up all items of that type. Each value of this
 * enum has an equivalent node in the graph to which
 * individual permission grant nodes refer if they
 * pertain to an entire class of items rather than a
 * single item.
 */
public enum ContentTypes {
    DOCUMENTARY_UNIT(Entities.DOCUMENTARY_UNIT),
    REPOSITORY(Entities.REPOSITORY),
    AUTHORITY(Entities.HISTORICAL_AGENT),
    GROUP(Entities.GROUP),
    USER_PROFILE(Entities.USER_PROFILE),
    ANNOTATION(Entities.ANNOTATION),
    SYSTEM_EVENT(Entities.SYSTEM_EVENT),
    AUTHORITATIVE_SET(Entities.AUTHORITATIVE_SET),
    CVOC_VOCABULARY(Entities.CVOC_VOCABULARY),
    CVOC_CONCEPT(Entities.CVOC_CONCEPT),
    LINK(Entities.LINK),
    COUNTRY(Entities.COUNTRY),
    VIRTUAL_COLLECTION(Entities.VIRTUAL_UNIT);
    
    private final String name;
    
    ContentTypes(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @Override public String toString() {
        return name;
    }
    
    @JsonCreator
    public static ContentTypes withName(String name) {
        for (ContentTypes c : ContentTypes.values()) {
            if (c.getName().equals(name))
                return c;
        }
        throw new IllegalArgumentException("Invalid content type type: '" + name + "'");
    }
}
