/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.models.cvoc;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 *
 * @author linda
 */
public interface AuthoritativeItem extends AccessibleEntity {
    @Fetch(AuthoritativeSet.IN_SET)
    @Adjacency(label = Vocabulary.IN_CVOC)
    public AuthoritativeSet getAuthoritativeSet();

    @Adjacency(label = AuthoritativeSet.IN_SET)
    public void setAuthoritativeSet(final AuthoritativeSet authoritativeSet);
}
