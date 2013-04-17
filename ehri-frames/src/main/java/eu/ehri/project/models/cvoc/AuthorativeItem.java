/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.models.cvoc;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.annotations.Fetch;

/**
 *
 * @author linda
 */
public interface AuthorativeItem {
    @Fetch(AuthorativeSet.IN_SET)
    @Adjacency(label = Vocabulary.IN_CVOC)
    public AuthorativeSet getAuthorativeSet();

    @Adjacency(label = AuthorativeSet.IN_SET)
    public void setAuthorativeSet(final AuthorativeSet authorativeSet); 
}
