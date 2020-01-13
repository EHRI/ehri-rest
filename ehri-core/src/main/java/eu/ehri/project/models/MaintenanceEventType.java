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

package eu.ehri.project.models;

import eu.ehri.project.definitions.EventTypes;

/**
 * Values corresponding to the EAC-CPF 2010 and EAG 2012
 * XML schema.
 */
public enum MaintenanceEventType {
    created, revised, deleted, cancelled, derived, updated;

    // Old values before alignment with EAC-CPF
    private enum OldValues {
        create, creation, update, delete
    }

    public static MaintenanceEventType withName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            OldValues compatVal = OldValues.valueOf(name);
            switch (compatVal) {
                case creation:
                case create:
                    return created;
                case delete:
                    return deleted;
                default:
                    return updated;
            }
        }
    }

    /**
     * Map EAC/EAG maintenance event type from system event types.
     *
     * @param eventType the system event type
     * @return the equivalent maintenance event type
     */
    public static MaintenanceEventType fromSystemEventType(EventTypes eventType) {
        switch (eventType) {
            case deletion:
                return deleted;
            case ingest:
            case creation:
                return created;
            default:
                return updated;
        }
    }
}
