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

import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Vertex;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.utils.EmptyIterable;

/**
 * Implementation of an anonymous user singleton.
 */
public enum AnonymousAccessor implements Accessor {

    INSTANCE;

    /**
     * Obtain the shared instance of the Anonymous Accessor.
     * There Can Be Only One.
     */
    public static Accessor getInstance() {
        return INSTANCE;
    }

    @Override
    public <T extends Entity> T as(Class<T> cls) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public boolean isAnonymous() {
        return true;
    }

    @Override
    public String getId() {
        return Group.ANONYMOUS_GROUP_IDENTIFIER;
    }

    @Override
    public String getType() {
        return Entities.GROUP;
    }

    @Override
    public Vertex asVertex() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return Group.ANONYMOUS_GROUP_IDENTIFIER;
    }

    @Override
    public Iterable<Accessor> getParents() {
        return new EmptyIterable<>();
    }

    @Override
    public Iterable<Accessor> getAllParents() {
        return new EmptyIterable<>();
    }

    @Override
    public Iterable<PermissionGrant> getPermissionGrants() {
        return new EmptyIterable<>();
    }

    @Override
    public void addPermissionGrant(PermissionGrant grant) {
        throw new UnsupportedOperationException();
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
