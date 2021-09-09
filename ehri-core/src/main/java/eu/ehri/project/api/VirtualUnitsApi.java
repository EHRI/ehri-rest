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
