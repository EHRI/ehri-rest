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

package eu.ehri.project.views;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.persistence.TraversalCallback;

import java.util.Set;


public class DescriptionViews <E extends Described> {

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final Crud<E> crud;
    private final ViewHelper helper;
    private final Serializer serializer;
    private final ActionManager actionManager;

    public DescriptionViews(FramedGraph<?> graph, Class<E> parentClass) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.crud = ViewFactory.getCrudNoLogging(graph, parentClass);
        this.helper = new ViewHelper(graph);
        this.serializer = new Serializer.Builder(graph).dependentOnly().build();
        this.actionManager = new ActionManager(graph);
    }

    public int delete(String parentId, String id, Accessor user, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, SerializationError {
        E parent = crud.detail(parentId, user);
        Accessible dependentItem = manager.getEntity(id, Accessible.class);
        if (!itemsInSubtree(parent).contains(dependentItem)) {
            throw new PermissionDenied("Given description does not belong to its parent item");
        }
        helper.checkEntityPermission(parent, user, PermissionType.UPDATE);
        actionManager.newEventContext(parent, user.as(Actioner.class),
                EventTypes.deleteDependent, logMessage)
                .createVersion(dependentItem)
                .commit();
        return getPersister(parent)
                .delete(serializer.entityToBundle(dependentItem));
    }

    public <T extends Accessible> T create(String parentId, Bundle data,
            Class<T> descriptionClass, Accessor user, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, ValidationError {
        E parent = crud.detail(parentId, user);
        helper.checkEntityPermission(parent, user, PermissionType.UPDATE);
        T out = getPersister(parent).create(data, descriptionClass);
        actionManager.newEventContext(parent, user.as(Actioner.class),
                EventTypes.createDependent, logMessage)
                .commit();
        return out;
    }

    public <T extends Accessible> Mutation<T> update(String parentId, Bundle data,
                Class<T> descriptionClass, Accessor user, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, ValidationError {
        E parent = crud.detail(parentId, user);
        helper.checkEntityPermission(parent, user, PermissionType.UPDATE);
        Mutation<T> out = getPersister(parent).update(data, descriptionClass);
        if (!out.unchanged()) {
            actionManager
                    .newEventContext(parent, user.as(Actioner.class),
                            EventTypes.modifyDependent, logMessage)
                    .createVersion(out.getNode(), out.getPrior().get())
                    .commit();
        }
        return out;
    }

    // Helpers
    private BundleDAO getPersister(PermissionScope scope) {
        return new BundleDAO(graph, scope.idPath());
    }

    private Set<Entity> itemsInSubtree(Entity topLevel) {
        final Set<Entity> items = Sets.newHashSet();
        serializer.traverseSubtree(topLevel, new TraversalCallback() {
            @Override
            public void process(Entity frame, int depth, String relation, int relationIndex) {
                items.add(frame);
            }
        });
        return items;
    }
}
