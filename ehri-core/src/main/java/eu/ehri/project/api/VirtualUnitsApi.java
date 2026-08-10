/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.api;

import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.VirtualUnit;

public interface VirtualUnitsApi {

    /**
     * Move units included in one virtual collection to another virtual collection.
     *
     * @param from     the ID of the source VC
     * @param to       the ID of the target VC
     * @param included a list of unit IDs to move
     * @throws PermissionDenied if the action cannot be performed by the current user
     */
    void moveIncludedUnits(VirtualUnit from, VirtualUnit to, Iterable<DocumentaryUnit> included)
            throws PermissionDenied;

    /**
     * Add documentary units to be included in a virtual unit as child items.
     *
     * @param parent   the parent VU
     * @param included a set of child DUs
     * @return the parent VU
     * @throws PermissionDenied if the action cannot be performed by the current user
     */
    VirtualUnit addIncludedUnits(VirtualUnit parent, Iterable<DocumentaryUnit> included)
            throws PermissionDenied;

    /**
     * Remove documentary units from a virtual unit as child items.
     *
     * @param parent   the parent VC
     * @param included a set of child DUs
     * @return the parent VU
     * @throws PermissionDenied if the action cannot be performed by the current user
     */
    VirtualUnit removeIncludedUnits(VirtualUnit parent, Iterable<DocumentaryUnit> included)
            throws PermissionDenied;
}
