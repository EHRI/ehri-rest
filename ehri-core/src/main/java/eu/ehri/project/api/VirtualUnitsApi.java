package eu.ehri.project.api;

import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.Entity;

public interface VirtualUnitsApi {

    /**
     * Move units included in one virtual collection to another virtual collection.
     *
     * @param from     the ID of the source VC
     * @param to       the ID of the target VC
     * @param included a list of unit IDs to move
     */
    void moveIncludedUnits(VirtualUnit from, VirtualUnit to, Iterable<DocumentaryUnit> included)
            throws PermissionDenied;

    /**
     * Add documentary units to be included in a virtual unit as child items.
     *
     * @param parent   the parent VU
     * @param included a set of child DUs
     * @return the parent VU
     */
    VirtualUnit addIncludedUnits(VirtualUnit parent, Iterable<DocumentaryUnit> included)
            throws PermissionDenied;

    /**
     * Remove documentary units from a virtual unit as child items.
     *
     * @param parent   the parent VC
     * @param included a set of child DUs
     * @return the parent VU
     */
    VirtualUnit removeIncludedUnits(VirtualUnit parent, Iterable<DocumentaryUnit> included)
            throws PermissionDenied;

    /**
     * Find virtual collections to which this item belongs.
     *
     * @param item an item (typically a documentary unit)
     * @return a set of top-level virtual units
     */
    Iterable<VirtualUnit> getVirtualCollections(Entity item);
}
