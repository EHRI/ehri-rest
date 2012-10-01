package eu.ehri.project.importers;

import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Functor class
 * 
 * @author michaelb
 * 
 */
public interface ImportCallback {
    public void itemImported(final AccessibleEntity item);
}
