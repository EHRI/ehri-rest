package eu.ehri.project.importers;

import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.persistence.Mutation;

/**
 * Functor class
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 * 
 */
public interface ImportCallback {
    public void itemImported(final Mutation<? extends AccessibleEntity> mutation);
}
