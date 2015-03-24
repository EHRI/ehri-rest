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
package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;

/**
 * Frame class representing a grant of a permission
 * to a user.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.PERMISSION_GRANT)
public interface PermissionGrant extends Frame {

    @Fetch(value = Ontology.PERMISSION_GRANT_HAS_SUBJECT, ifBelowLevel = 1, numLevels = 1)
    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_SUBJECT)
    public Accessor getSubject();

    @Fetch(value = Ontology.PERMISSION_GRANT_HAS_GRANTEE, ifBelowLevel = 1, numLevels = 1)
    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_GRANTEE)
    public Accessor getGrantee();
    
    @Fetch(value = Ontology.PERMISSION_GRANT_HAS_TARGET, ifBelowLevel = 1, numLevels = 1)
    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_TARGET)
    public Iterable<PermissionGrantTarget> getTargets();

    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_TARGET)
    public void addTarget(final PermissionGrantTarget target);

    @Fetch(Ontology.PERMISSION_GRANT_HAS_PERMISSION)
    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_PERMISSION)
    public Permission getPermission();

    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_PERMISSION)
    public void setPermission(final Permission permission);

    @Fetch(value = Ontology.PERMISSION_GRANT_HAS_SCOPE, ifBelowLevel = 1, numLevels = 0)
    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_SCOPE)
    public PermissionScope getScope();
    
    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_SCOPE)
    public void setScope(final PermissionScope scope);

    @Adjacency(label = Ontology.PERMISSION_GRANT_HAS_SCOPE)
    public void removeScope(final PermissionScope scope);
}
