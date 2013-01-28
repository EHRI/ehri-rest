package eu.ehri.project.views;

import com.google.common.collect.ListMultimap;

import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.base.Accessor;

public interface Annotations {

    /**
     * Fetch annotations for an item's subtree.
     * 
     * @param id
     * @return
     * @throws ItemNotFound
     */
    public ListMultimap<String, Annotation> getFor(String id, Accessor accessor)
            throws ItemNotFound;
}
