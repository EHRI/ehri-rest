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

package eu.ehri.project.api.impl;

import com.google.common.collect.Lists;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.PermissionUtils;
import eu.ehri.project.api.VirtualUnitsApi;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Accessor;

import java.util.List;

/**
 * View class for interacting with virtual units.
 */
class VirtualUnitsApiImpl implements VirtualUnitsApi {

    private final FramedGraph<?> graph;
    private final GraphManager manager;
    private final PermissionUtils viewHelper;
    private final Accessor accessor;

    VirtualUnitsApiImpl(FramedGraph<?> graph, Accessor accessor) {
        this.graph = graph;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.viewHelper = new PermissionUtils(graph);
        this.accessor = accessor;
    }

    @Override
    public void moveIncludedUnits(VirtualUnit from, VirtualUnit to, Iterable<DocumentaryUnit> included) throws PermissionDenied {
        // Wrap this in a list as a precaution because it
        // will be iterated twice!
        List<DocumentaryUnit> items = Lists.newArrayList(included);
        removeIncludedUnits(from, items);
        addIncludedUnits(to, items);
    }

    @Override
    public VirtualUnit addIncludedUnits(VirtualUnit parent, Iterable<DocumentaryUnit> included)
            throws PermissionDenied {
        viewHelper.checkEntityPermission(parent, accessor, PermissionType.UPDATE);
        for (DocumentaryUnit unit : included) {
            parent.addIncludedUnit(unit);
        }
        return parent;
    }

    @Override
    public VirtualUnit removeIncludedUnits(VirtualUnit parent, Iterable<DocumentaryUnit> included)
            throws PermissionDenied {
        viewHelper.checkEntityPermission(parent, accessor, PermissionType.UPDATE);
        for (DocumentaryUnit unit : included) {
            parent.removeIncludedUnit(unit);
        }
        return parent;
    }
}
