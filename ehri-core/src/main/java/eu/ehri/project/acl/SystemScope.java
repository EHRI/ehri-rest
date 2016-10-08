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

import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.PermissionScope;

import java.util.Collection;
import java.util.Collections;

/**
 * Singleton class representing the system scope for
 * permissions and ID namespaces.
 */
public enum SystemScope implements PermissionScope {

    INSTANCE;

    /**
     * Obtain the shared instance of SystemScope.
     *
     * @return The global SystemScope instance
     */
    public static PermissionScope getInstance() {
        return INSTANCE;
    }

    @Override
    public <T extends Entity> T as(Class<T> cls) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        return Entities.SYSTEM;
    }

    @Override
    public String getType() {
        return Entities.SYSTEM;
    }

    @Override
    public String getIdentifier() {
        return Entities.SYSTEM;
    }

    @Override
    public Vertex asVertex() {
        return null;
    }

    @Override
    public Iterable<PermissionGrant> getPermissionGrants() {
        return Collections.emptyList();
    }

    public Iterable<PermissionScope> getPermissionScopes() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<Accessible> getContainedItems() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<Accessible> getAllContainedItems() {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> idPath() {
        return Collections.emptyList();
    }

    @Override
    public <T> T getProperty(String key) {
        return null;
    }

    @Override
    public <T> T getProperty(Enum<?> key) {
        return null;
    }

    @Override
    public java.util.Set<String> getPropertyKeys() {
        return Collections.emptySet();
    }
}
