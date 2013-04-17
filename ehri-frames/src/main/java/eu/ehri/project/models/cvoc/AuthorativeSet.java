/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.models.cvoc;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.PermissionScope;

/**
 *
 * @author linda
 */
public interface AuthorativeSet extends AccessibleEntity, IdentifiableEntity, PermissionScope {
    public static final String LABEL = "authorativelist";
    public static final String IN_SET = "inAuthorativeList";

    @Adjacency(label = IN_SET, direction = Direction.IN)
    public Iterable<AuthorativeItem> getAuthorativeItems();

    @Adjacency(label = IN_SET, direction = Direction.IN)
    public void addAuthorativeItem(final AuthorativeItem authorativeItem);
    
}
