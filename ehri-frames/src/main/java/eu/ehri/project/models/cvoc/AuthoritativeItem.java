/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.models.cvoc;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 *
 * @author linda
 */
public interface AuthoritativeItem extends AccessibleEntity {
    @Fetch(Ontology.ITEM_IN_AUTHORITATIVE_SET)
    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET)
    public AuthoritativeSet getAuthoritativeSet();

    @Adjacency(label = Ontology.ITEM_IN_AUTHORITATIVE_SET)
    public void setAuthoritativeSet(final AuthoritativeSet authoritativeSet);
}
