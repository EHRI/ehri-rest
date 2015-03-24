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

import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.persistence.ActionManager;

/**
 * View class for managing item promotion.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class PromotionViews implements Scoped<PromotionViews> {

    private final ViewHelper helper;
    private final ActionManager actionManager;
    private final FramedGraph<?> graph;

    public static class NotPromotableError extends Exception {
        public NotPromotableError(String itemId) {
            super(String.format("Item '%s' is not marked as promotable.", itemId));
        }
    }

    /**
     * Scoped constructor.
     *
     * @param graph A framed graph
     * @param scope An item scope
     */
    public PromotionViews(FramedGraph<?> graph, PermissionScope scope) {
        this.graph = graph;
        helper = new ViewHelper(graph, scope);
        actionManager = new ActionManager(graph, scope);
    }

    /**
     * Constructor with system scope.
     *
     * @param graph A framed graph
     */
    public PromotionViews(FramedGraph<?> graph) {
        this(graph, SystemScope.getInstance());
    }

    /**
     * Up vote an item, removing a down vote if there is one.
     *
     * @param item The promotable item
     * @param user The item's promoter
     * @throws PermissionDenied
     */
    public void upVote(Promotable item, UserProfile user) throws PermissionDenied,
            NotPromotableError {
        helper.checkEntityPermission(item, user, PermissionType.PROMOTE);
        if (!item.isPromotable()) {
            throw new NotPromotableError(item.getId());
        }
        item.addPromotion(user);
        actionManager.logEvent(item, user, EventTypes.promotion);
    }

    /**
     * Remove an up vote.
     *
     * @param item The promotable item
     * @param user The item's promoter
     * @throws PermissionDenied
     */
    public void removeUpVote(Promotable item, UserProfile user) throws PermissionDenied {
        item.removePromotion(user);
    }

    /**
     * Down vote an item, removing an up vote if there is one.
     *
     * @param item The promotable item
     * @param user The item's promoter
     * @throws PermissionDenied
     */
    public void downVote(Promotable item, UserProfile user) throws PermissionDenied,
            NotPromotableError {
        helper.checkEntityPermission(item, user, PermissionType.PROMOTE);
        if (!item.isPromotable()) {
            throw new NotPromotableError(item.getId());
        }
        item.addDemotion(user);
        actionManager.logEvent(item, user, EventTypes.demotion);
    }

    /**
     * Remove a down vote.
     *
     * @param item The promotable item
     * @param user The item's promoter
     * @throws PermissionDenied
     */
    public void removeDownVote(Promotable item, UserProfile user) throws PermissionDenied {
        item.removeDemotion(user);
    }

    @Override
    public PromotionViews withScope(PermissionScope scope) {
        return new PromotionViews(graph, scope);
    }
}
