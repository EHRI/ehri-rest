package eu.ehri.project.importers;

import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Functor class
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 * 
 */
public interface ImportCallback {
    public void itemImported(final AccessibleEntity item);
}
