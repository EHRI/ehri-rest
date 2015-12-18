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

package eu.ehri.project.exceptions;

/**
 * Represents a violation of the permission system constraints
 * on the modification of data.
 */
public class PermissionDenied extends Exception {

    private static final long serialVersionUID = -3948097018322416889L;

    private String accessor;
    private String entity;
    private String scope;
    private String permission;

    public PermissionDenied(String accessor, String message) {
        super(String.format("Permission denied accessing resource as '%s': %s",
                accessor, message));
        this.accessor = accessor;
    }

    public PermissionDenied(String message) {
        super(message);
    }

    public PermissionDenied(String accessor, String entity, String message) {
        super(String.format(
                "Permission denied accessing resource '%s' as '%s': %s",
                entity, accessor, message));
        this.accessor = accessor;
        this.entity = entity;
    }

    public PermissionDenied(String accessor, String entity,
            String permission, String scope) {
        super(
                String.format(
                        "Permission '%s' denied for resource '%s' as '%s' with scope '%s'",
                        permission, entity,
                        accessor, scope));
        this.accessor = accessor;
        this.entity = entity;
        this.scope = scope;
        this.permission = permission;
    }

    public String getAccessor() {
        return accessor;
    }

    public String getEntity() {
        return entity;
    }

    public String getScope() {
        return scope;
    }

    public String getPermission() {
        return permission;
    }
}
