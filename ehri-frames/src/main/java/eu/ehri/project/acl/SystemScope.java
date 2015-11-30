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
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.EmptyIterable;

import java.util.Collection;

/**
 * Singleton class representing the system scope for
 * permissions and ID namespaces.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 */
public enum SystemScope implements PermissionScope {
    
    INSTANCE;

    /**
     * Obtain the shared instance of SystemScope.
     * @return The global SystemScope instance
     */
    public static PermissionScope getInstance() {
        return INSTANCE;
    }
    
    public String getId() {
        return Entities.SYSTEM;
    }

    public String getType() {
        return Entities.SYSTEM;
    }

    public String getIdentifier() {
        return Entities.SYSTEM;
    }

    public Vertex asVertex() {
        // TODO: Determine if there's a better approach to this.
        // Since PermissionScope can be implemented by several
        // types of node, comparing them by vertex is the only
        // reliable approach. ReRally, this operation should
        // throw an UnsupportedOperationException().
        return null;
    }

    public Iterable<PermissionGrant> getPermissionGrants() {
        return new EmptyIterable<>();
    }

    public Iterable<PermissionScope> getPermissionScopes() {
        return new EmptyIterable<>();
    }

    @Override
    public Iterable<AccessibleEntity> getContainedItems() {
        return new EmptyIterable<>();
    }

    @Override
    public Iterable<AccessibleEntity> getAllContainedItems() {
        return new EmptyIterable<>();
    }

    @Override
    public Collection<String> idPath() {
        return Lists.newArrayList();
    }

    @Override
    public <T> T getProperty(String key) {
        return null;
    }

    @Override
    public java.util.Set<String> getPropertyKeys() {
        return Sets.newHashSet();
    }
}
