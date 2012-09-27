package eu.ehri.project.importers;

import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Functor class
 * 
 * @author michaelb
 * 
 */
public interface CreationCallback {
    public void itemCreated(final AccessibleEntity item);
}
