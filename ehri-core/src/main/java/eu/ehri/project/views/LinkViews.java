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
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleDAO;

import java.util.Collection;
import java.util.List;

/**
 * View class for handling annotation-related operations.
 */
public final class LinkViews implements Scoped<LinkViews> {

    private final FramedGraph<?> graph;
    private final ViewHelper helper;
    private final AclManager acl;
    private final GraphManager manager;

    /**
     * Scoped constructor.
     *
     * @param graph The framed graph
     * @param scope The permission scope
     */
    public LinkViews(FramedGraph<?> graph, PermissionScope scope) {
        this.graph = graph;
        helper = new ViewHelper(graph, scope);
        acl = helper.getAclManager();
        manager = GraphManagerFactory.getInstance(graph);
    }

    /**
     * Constructor with system scope.
     *
     * @param graph The framed graph
     */
    public LinkViews(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Create a link between two items.
     *
     * @param targetId1 the identifier of a Accessible target of this Annotation
     * @param targetId2 the identifier of a Annotator source of this Annotation
     * @param bundle    the annotation itself
     * @param user the current user
     * @return A new link
     * @throws eu.ehri.project.exceptions.ItemNotFound
     *
     * @throws eu.ehri.project.exceptions.ValidationError
     *
     * @throws eu.ehri.project.exceptions.PermissionDenied
     *
     */
    public Link create(String targetId1, String targetId2, List<String> bodies, Bundle bundle,
            UserProfile user, Collection<Accessor> accessibleTo) throws ItemNotFound, ValidationError, PermissionDenied {
        Linkable t1 = manager.getEntity(targetId1, Linkable.class);
        Linkable t2 = manager.getEntity(targetId2, Linkable.class);
        helper.checkEntityPermission(t1, user, PermissionType.ANNOTATE);
        // TODO: Should this require perms to link another item???
        //helper.checkEntityPermission(t2, user, PermissionType.ANNOTATE);
        Link link = new BundleDAO(graph).create(bundle, Link.class);
        link.addLinkTarget(t1);
        link.addLinkTarget(t2);
        link.setLinker(user);
        acl.setAccessors(link, accessibleTo);
        ActionManager.EventContext eventContext = new ActionManager(graph, t1).newEventContext(
                graph.frame(user.asVertex(), Actioner.class),
                EventTypes.link, Optional.<String>absent())
                .addSubjects(link)
                .addSubjects(t2);
        for (String body : bodies) {
            Accessible item = manager.getEntity(body, Accessible.class);
            link.addLinkBody(item);
            eventContext.addSubjects(item);
        }
        eventContext.commit();
        return link;
    }

    /**
     * Create a link between two items, along with an access point on the given description.
     *
     * @param targetId1       the identifier of a Accessible target of this Annotation
     * @param targetId2 the identifier of a Annotator source of this Annotation
     * @param bundle   the annotation itself
     * @param user The current user
     * @return A new link
     * @throws ItemNotFound
     * @throws ValidationError
     * @throws PermissionDenied
     */
    public Link createAccessPointLink(String targetId1, String targetId2, String descriptionId, String bodyName,
            String bodyType, Bundle bundle,
            UserProfile user, Collection<Accessor> accessibleTo) throws ItemNotFound, ValidationError, PermissionDenied {
        Linkable t1 = manager.getEntity(targetId1, Linkable.class);
        Linkable t2 = manager.getEntity(targetId2, Linkable.class);
        Description description = manager.getEntity(descriptionId, Description.class);
        helper.checkEntityPermission(t1, user, PermissionType.ANNOTATE);
        // TODO: Should this require perms to link another item???
        //helper.checkEntityPermission(t2, user, PermissionType.ANNOTATE);
        helper.checkEntityPermission(description.getEntity(), user, PermissionType.UPDATE);
        Link link = new BundleDAO(graph).create(bundle, Link.class);
        Bundle relBundle = new Bundle(EntityClass.ACCESS_POINT)
                .withDataValue(Ontology.NAME_KEY, bodyName)
                .withDataValue(Ontology.ACCESS_POINT_TYPE, bodyType)
                .withDataValue(Ontology.LINK_HAS_DESCRIPTION, link.getDescription());
        AccessPoint rel = new BundleDAO(graph).create(relBundle, AccessPoint.class);
        description.addAccessPoint(rel);
        link.addLinkTarget(t1);
        link.addLinkTarget(t2);
        link.setLinker(user);
        link.addLinkBody(rel);
        acl.setAccessors(link, accessibleTo);
        ActionManager.EventContext eventContext = new ActionManager(graph).newEventContext(
                t1, graph.frame(user.asVertex(), Actioner.class), EventTypes.link);
        eventContext.addSubjects(link).addSubjects(t2).addSubjects(rel);
        eventContext.commit();
        return link;
    }

    @Override
    public LinkViews withScope(PermissionScope scope) {
        return new LinkViews(graph, scope);
    }
}
